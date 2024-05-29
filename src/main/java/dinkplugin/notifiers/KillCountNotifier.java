package dinkplugin.notifiers;

import dinkplugin.message.Embed;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.KillCountService;
import dinkplugin.util.TimeUtils;
import dinkplugin.util.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
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

    private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion)\\s?count is: (?<value>\\d+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>\\d+)\\b");
    private static final Pattern TIME_REGEX = Pattern.compile("(?:Duration|time|Subdued in):? (?<time>[\\d:]+(.\\d+)?)\\.?", Pattern.CASE_INSENSITIVE);

    private static final String BA_BOSS_NAME = "Penance Queen";

    /**
     * The maximum number of ticks to hold onto a fight duration without a corresponding boss name.
     * <p>
     * Note: unlike other notifiers, this is applied asymmetrically
     * (i.e., we do not wait for fight duration if only boss name was received on the tick)
     */
    @VisibleForTesting
    static final int MAX_BAD_TICKS = 10;

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
            parse(message).ifPresent(this::updateData);
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
                this.data.set(new BossNotificationData(BA_BOSS_NAME, gambleCount, "The Queen is dead!", null, null));
            }
        }
    }

    public void onTick() {
        BossNotificationData data = this.data.get();
        if (data != null) {
            if (data.getBoss() != null) {
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
        boolean ba = data.getBoss().equals(BA_BOSS_NAME);
        if (!checkKillInterval(data.getCount(), data.isPersonalBest()) && !ba)
            return;

        // Assemble content
        boolean isPb = data.isPersonalBest() == Boolean.TRUE;
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

        // Prepare body
        NotificationBody.NotificationBodyBuilder<BossNotificationData> body =
            NotificationBody.<BossNotificationData>builder()
                .text(content)
                .extra(data)
                .playerName(player)
                .type(NotificationType.KILL_COUNT);

        // Add embed if not screenshotting
        boolean screenshot = config.killCountSendImage();
        if (!screenshot) {
            if (ba) {
                body.embeds(Collections.singletonList(Embed.ofImage(ItemUtils.getNpcImageUrl(NpcID.PENANCE_QUEEN))));
            } else {
                Arrays.stream(client.getCachedNPCs())
                    .filter(Objects::nonNull)
                    .filter(npc -> data.getBoss().equalsIgnoreCase(npc.getName()))
                    .findAny()
                    .map(NPC::getId)
                    .map(ItemUtils::getNpcImageUrl)
                    .map(Embed::ofImage)
                    .map(Collections::singletonList)
                    .ifPresent(body::embeds);
            }
        }

        // Call webhook
        createMessage(screenshot, body.build());
    }

    private boolean checkKillInterval(int killCount, @Nullable Boolean pb) {
        if (pb == Boolean.TRUE && config.killCountNotifyBestTime())
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
                return new BossNotificationData(
                    defaultIfNull(updated.getBoss(), old.getBoss()),
                    defaultIfNull(updated.getCount(), old.getCount()),
                    defaultIfNull(updated.getGameMessage(), old.getGameMessage()),
                    defaultIfNull(updated.getTime(), old.getTime()),
                    defaultIfNull(updated.isPersonalBest(), old.isPersonalBest())
                );
            }
        });
    }

    private static Optional<BossNotificationData> parse(String message) {
        if (message.startsWith("Preparation")) return Optional.empty();
        Optional<Pair<String, Integer>> boss = parseBoss(message);
        if (boss.isPresent())
            return boss.map(pair -> new BossNotificationData(pair.getLeft(), pair.getRight(), message, null, null));
        return parseTime(message).map(t -> new BossNotificationData(null, null, null, t.getLeft(), t.getRight()));
    }

    private static Optional<Pair<Duration, Boolean>> parseTime(String message) {
        Matcher matcher = TIME_REGEX.matcher(message);
        if (matcher.find()) {
            Duration duration = TimeUtils.parseTime(matcher.group("time"));
            boolean pb = message.toLowerCase().contains("(new personal best)");
            return Optional.of(Pair.of(duration, pb));
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
            return Optional.ofNullable(boss).map(k -> Pair.of(boss, Integer.parseInt(count)));
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
        if (raid.equalsIgnoreCase("Theatre of Blood")
            || raid.equalsIgnoreCase("Tombs of Amascut")
            || raid.equalsIgnoreCase("Chambers of Xeric")
            || raid.equalsIgnoreCase("Chambers of Xeric Challenge Mode"))
            return boss;

        return null;
    }
}
