package dinkplugin.notifiers;

import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Slf4j
@Singleton
public class KillCountNotifier extends BaseNotifier {
    private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion)\\s?count is: (?<value>\\d+)\\b");
    private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>\\d+)\\b");
    private static final Pattern TIME_REGEX = Pattern.compile("(?:Duration|time|Subdued in):? (?<time>[\\d:]+(.\\d+)?)\\.?", Pattern.CASE_INSENSITIVE);

    private BossNotificationData data;

    @Override
    public boolean isEnabled() {
        return config.notifyKillCount() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.killCountWebhook();
    }

    public void reset() {
        this.data = null;
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(message).ifPresent(this::updateData);
    }

    public void onTick() {
        if (data != null) {
            handleKill(data);
            reset();
        }
    }

    private void handleKill(BossNotificationData data) {
        if (data.getBoss() == null || data.getCount() == null || !checkKillInterval(data.getCount(), data.isPersonalBest()))
            return;

        // Assemble content
        String player = Utils.getPlayerName(client);
        String time = DurationFormatUtils.formatDurationHMS(defaultIfNull(data.getTime(), Duration.ZERO).toMillis());
        String content = StringUtils.replaceEach(
            data.isPersonalBest() == Boolean.TRUE ? config.killCountBestTimeMessage() : config.killCountMessage(),
            new String[] { "%USERNAME%", "%BOSS%", "%COUNT%", "%TIME%" },
            new String[] { player, data.getBoss(), String.valueOf(data.getCount()), time }
        );

        // Prepare body
        NotificationBody.NotificationBodyBuilder<BossNotificationData> body =
            NotificationBody.<BossNotificationData>builder()
                .content(content)
                .extra(data)
                .playerName(player)
                .screenshotFile("killCountImage.png")
                .type(NotificationType.KILL_COUNT);

        // Add embed if not screenshotting
        boolean screenshot = config.killCountSendImage();
        if (!screenshot)
            Arrays.stream(client.getCachedNPCs())
                .filter(Objects::nonNull)
                .filter(npc -> data.getBoss().equalsIgnoreCase(npc.getName()))
                .findAny()
                .map(NPC::getId)
                .map(Utils::getNpcImageUrl)
                .map(NotificationBody.UrlEmbed::new)
                .map(NotificationBody.Embed::new)
                .map(Collections::singletonList)
                .ifPresent(body::embeds);

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
        if (data == null) {
            this.data = updated;
        } else {
            this.data = new BossNotificationData(
                defaultIfNull(updated.getBoss(), data.getBoss()),
                defaultIfNull(updated.getCount(), data.getCount()),
                defaultIfNull(updated.getGameMessage(), data.getGameMessage()),
                defaultIfNull(updated.getTime(), data.getTime()),
                defaultIfNull(updated.isPersonalBest(), data.isPersonalBest())
            );
        }
    }

    private static Optional<BossNotificationData> parse(String message) {
        return parseBoss(message)
            .map(pair -> new BossNotificationData(pair.getLeft(), pair.getRight(), message, null, null))
            .map(Optional::of)
            .orElseGet(() -> parseTime(message).map(time -> new BossNotificationData(null, null, null, time.getLeft(), time.getRight())));
    }

    private static Optional<Pair<Duration, Boolean>> parseTime(String message) {
        Matcher matcher = TIME_REGEX.matcher(message);
        if (matcher.find()) {
            Duration duration = Utils.parseTime(matcher.group("time"));
            boolean pb = message.contains("(new personal best)");
            return Optional.of(Pair.of(duration, pb));
        }
        return Optional.empty();
    }

    @VisibleForTesting
    static Optional<Pair<String, Integer>> parseBoss(String message) {
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
        try {
            return Optional.ofNullable(boss).map(k -> Pair.of(boss, Integer.parseInt(count)));
        } catch (NumberFormatException e) {
            log.debug("Failed to parse kill count [{}] for boss [{}]", count, boss);
            return Optional.empty();
        }
    }

    @Nullable
    private static String parsePrimaryBoss(String boss, String type) {
        switch (type) {
            case "chest":
                return "Barrows".equalsIgnoreCase(boss) ? boss : null;

            case "completion":
                if ("Gauntlet".equalsIgnoreCase(boss))
                    return "Crystalline Hunllef";
                if ("Corrupted Gauntlet".equalsIgnoreCase(boss))
                    return "Corrupted Hunllef";
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
