package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.BossNotificationData;
import dinkplugin.util.KillCountService;
import dinkplugin.util.TimeUtils;
import dinkplugin.util.Utils;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Slf4j
@Singleton
public class KillCountNotifier extends BaseNotifier {

    @Varbit
    public static final int KILL_COUNT_SPAM_FILTER = 4930;
    public static final String SPAM_WARNING = "Kill Count Notifier requires disabling the in-game setting: Filter out boss kill-count with spam-filter";

    private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion|harvest)\\s?count is: ?(?<value>[\\d,]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>[\\d,]+)\\b");
    private static final Pattern TIME_REGEX = Pattern.compile("(?:Duration|time|Subdued in):? (?<time>[\\d:]+(?:.\\d+)?)\\.?(?: Personal best: (?<pbtime>[\\d:+]+(?:.\\d+)?))?", Pattern.CASE_INSENSITIVE);

    private static final String BA_BOSS_NAME = "Penance Queen";

    /**
     * The maximum number of ticks to hold onto a fight duration without a corresponding boss name.
     * <p>
     * Note: unlike other notifiers, this is applied asymmetrically
     * (i.e., we do not wait for fight duration if only boss name was received on the tick)
     */
    @VisibleForTesting
    static final int MAX_BAD_TICKS = 10;

    @Inject
    private KillCountService kcService;

    private final AtomicInteger badTicks = new AtomicInteger();
    private final AtomicReference<BossNotificationData> data = new AtomicReference<>();

    @Override
    public boolean isEnabled() {
        return config.notifyKillCount() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.killCountWebhook();
    }

    public void reset() {
        this.data.set(null);
        this.badTicks.set(0);
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(client, message).ifPresent(this::updateData);
    }

    public void onFriendsChatNotification(String message) {
        // For CoX, Jagex sends duration via FRIENDSCHATNOTIFICATION
        if (message.startsWith("Congratulations - your raid is complete!"))
            this.onGameMessage(message);
    }

    public void onWidget(WidgetLoaded event) {
        if (!isEnabled())
            return;

        // Barbarian Assault: Track Penance Queen kills
        if (config.killCountPenanceQueen() && event.getGroupId() == InterfaceID.BA_REWARD) {
            Widget widget = client.getWidget(ComponentID.BA_REWARD_REWARD_TEXT);
            // https://oldschool.runescape.wiki/w/Barbarian_Assault/Rewards#Earning_Honour_points
            if (widget != null && widget.getText().contains("80 ") && widget.getText().contains("5 ")) {
                int gambleCount = client.getVarbitValue(Varbits.BA_GC);
                this.data.set(new BossNotificationData(BA_BOSS_NAME, gambleCount, "The Queen is dead!", null, null, null, null));
            }
        }
    }

    public void onTick() {
        BossNotificationData data = this.data.get();
        if (data != null) {
            if (data.getBoss() != null && data.getCount() != null) {
                // ensure notifier was not disabled during bad ticks wait period
                if (isEnabled()) {
                    // once boss name has arrived, we notify at tick end (even if duration hasn't arrived)
                    handleKill(data);
                }
                reset();
            } else if (badTicks.incrementAndGet() > MAX_BAD_TICKS) {
                // after receiving fight duration, allow up to 10 ticks for boss name to arrive.
                // if boss name doesn't arrive in time, reset (to avoid stale data contaminating later notifications)
                reset();
            }
        }
    }

    private void handleKill(BossNotificationData data) {
        // ensure data is present
        if (data.getBoss() == null || data.getCount() == null)
            return;

        // ensure interval met or pb or ba, depending on config
        boolean isPb = data.isPersonalBest() == Boolean.TRUE;
        boolean ba = data.getBoss().equals(BA_BOSS_NAME);
        if (!checkKillInterval(data.getCount(), isPb) && !ba)
            return;

        // populate personalBest if absent
        if (data.getPersonalBest() == null && !isPb) {
            Duration pb = kcService.getPb(data.getBoss());
            if (pb != null && (data.getTime() == null || pb.compareTo(data.getTime()) < 0)) {
                data = data.withPersonalBest(pb);
            }
        }

        // Assemble content
        String player = Utils.getPlayerName(client);
        String time = TimeUtils.format(data.getTime(), TimeUtils.isPreciseTiming(client));
        Template content = Template.builder()
            .template(isPb ? config.killCountBestTimeMessage() : config.killCountMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(player))
            .replacement("%BOSS%", Replacements.ofWiki(data.getBoss()))
            .replacement("%COUNT%", Replacements.ofText(data.getCount() + (ba ? " high gambles" : "")))
            .replacement("%TIME%", Replacements.ofText(time))
            .build();

        // Call webhook
        createMessage(config.killCountSendImage(), NotificationBody.builder()
            .text(content)
            .extra(data)
            .playerName(player)
            .type(NotificationType.KILL_COUNT)
            .build());
    }

    private boolean checkKillInterval(int killCount, boolean pb) {
        if (pb && config.killCountNotifyBestTime())
            return true;

        if (killCount == 1 && config.killCountNotifyInitial())
            return true;

        int interval = config.killCountInterval();
        return interval <= 1 || killCount % interval == 0;
    }

    private void updateData(BossNotificationData updated) {
        data.getAndUpdate(old -> {
            if (old == null) {
                return updated;
            } else {
                // Boss data and timing are sent in separate messages
                // where the order of the messages differs depending on the boss.
                // Here, we update data without setting any not-null values back to null.
                String boss = defaultIfNull(updated.getBoss(), old.getBoss());
                boolean tob = boss != null && boss.startsWith("Theatre of Blood"); // prefer challenge time message that comes first: https://github.com/pajlads/DinkPlugin/issues/585
                return new BossNotificationData(
                    boss,
                    defaultIfNull(updated.getCount(), old.getCount()),
                    defaultIfNull(updated.getGameMessage(), old.getGameMessage()),
                    updated.getTime() == null || (tob && old.getTime() != null) ? old.getTime() : updated.getTime(),
                    updated.isPersonalBest() == null || (tob && old.isPersonalBest() != null) ? old.isPersonalBest() : updated.isPersonalBest(),
                    updated.getPersonalBest() == null || (tob && old.getPersonalBest() != null) ? old.getPersonalBest() : updated.getPersonalBest(),
                    defaultIfNull(updated.getParty(), old.getParty())
                );
            }
        });
    }

    private static Optional<BossNotificationData> parse(Client client, String message) {
        if (message.startsWith("Preparation")) return Optional.empty();
        Optional<Pair<String, Integer>> boss = parseBoss(message);
        if (boss.isPresent())
            return boss.map(pair -> new BossNotificationData(pair.getLeft(), pair.getRight(), message, null, null, null, Utils.getBossParty(client, pair.getLeft())));

        // TOB reports final wave duration before challenge time in the same message; skip to the part we care about
        int tobIndex = message.startsWith("Wave") ? message.indexOf(KillCountService.TOB) : -1;
        String msg = tobIndex < 0 ? message : message.substring(tobIndex);

        return parseTime(msg).map(t -> new BossNotificationData(tobIndex < 0 ? null : KillCountService.TOB, null, null, t.getTime(), t.isPb(), t.getPb(), null));
    }

    private static Optional<ParsedTime> parseTime(String message) {
        Matcher matcher = TIME_REGEX.matcher(message);
        if (matcher.find()) {
            Duration duration = TimeUtils.parseTime(matcher.group("time"));
            boolean isPb = message.toLowerCase().contains("(new personal best)");
            String pbTime = matcher.group("pbtime");
            Duration pb = pbTime != null ? TimeUtils.parseTime(pbTime) : null;
            return Optional.of(new ParsedTime(duration, isPb, pb));
        }
        return Optional.empty();
    }

    public static Optional<Pair<String, Integer>> parseBoss(String message) {
        Matcher primary = PRIMARY_REGEX.matcher(message);
        Matcher secondary; // lazy init
        if (primary.find()) {
            String boss = parsePrimaryBoss(primary.group("key"), primary.group("type"));
            String count = primary.group("value");
            return result(boss, count);
        } else if ((secondary = SECONDARY_REGEX.matcher(message)).find()) {
            String key = parseSecondary(secondary.group("key"));
            String value = secondary.group("value");
            return result(key, value);
        }
        return Optional.empty();
    }

    private static Optional<Pair<String, Integer>> result(String boss, String count) {
        // safely transform (String, String) => (String, Int)
        try {
            return Optional.ofNullable(boss).map(k -> Pair.of(boss, Integer.parseInt(count.replace(",", ""))));
        } catch (NumberFormatException e) {
            log.debug("Failed to parse kill count [{}] for boss [{}]", count, boss);
            return Optional.empty();
        }
    }

    @Nullable
    private static String parsePrimaryBoss(String boss, String type) {
        switch (type.toLowerCase()) {
            case "chest":
                if ("Barrows".equalsIgnoreCase(boss))
                    return boss;
                if ("Lunar".equals(boss))
                    return boss + " " + type;
                return null;

            case "completion":
                if (KillCountService.GAUNTLET_NAME.equalsIgnoreCase(boss))
                    return KillCountService.GAUNTLET_BOSS;
                if (KillCountService.CG_NAME.equalsIgnoreCase(boss))
                    return KillCountService.CG_BOSS;
                return null;

            case "harvest":
                if (KillCountService.HERBIBOAR.equalsIgnoreCase(boss))
                    return KillCountService.HERBIBOAR;

            case "kill":
                return boss;

            default:
                return null;
        }
    }

    private static String parseSecondary(String boss) {
        if (boss == null || "Wintertodt".equalsIgnoreCase(boss))
            return boss;

        int modeSeparator = boss.lastIndexOf(':');
        String raid = modeSeparator > 0 ? boss.substring(0, modeSeparator) : boss;
        if (raid.equalsIgnoreCase(KillCountService.TOB)
            || raid.equalsIgnoreCase(KillCountService.TOA)
            || raid.equalsIgnoreCase(KillCountService.COX)
            || raid.equalsIgnoreCase(KillCountService.COX + " Challenge Mode"))
            return boss;

        return null;
    }

    @Value
    private static class ParsedTime {
        Duration time;
        boolean isPb;
        @Nullable Duration pb;
    }
}
