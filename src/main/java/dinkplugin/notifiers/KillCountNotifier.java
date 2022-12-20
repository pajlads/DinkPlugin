package dinkplugin.notifiers;

import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.BossNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class KillCountNotifier extends BaseNotifier {
    private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion)\\s?count is: (?<value>\\d+)\\b");
    private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>\\d+)\\b");

    @Override
    public boolean isEnabled() {
        return config.notifyKillCount() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.killCountWebhook();
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(message).ifPresent(pair -> handleKill(pair.getLeft(), pair.getRight(), message));
    }

    private void handleKill(String boss, int killCount, String rawMessage) {
        if (!checkKillInterval(killCount)) return;

        // Assemble content
        String player = Utils.getPlayerName(client);
        String content = StringUtils.replaceEach(
            config.killCountMessage(),
            new String[] { "%USERNAME%", "%BOSS%", "%COUNT%" },
            new String[] { player, boss, String.valueOf(killCount) }
        );

        // Prepare body
        NotificationBody.NotificationBodyBuilder<BossNotificationData> body =
            NotificationBody.<BossNotificationData>builder()
                .content(content)
                .extra(new BossNotificationData(boss, killCount, rawMessage))
                .playerName(player)
                .screenshotFile("killCountImage.png")
                .type(NotificationType.KILL_COUNT);

        // Add embed if not screenshotting
        boolean screenshot = config.killCountSendImage();
        if (!screenshot)
            Arrays.stream(client.getCachedNPCs())
                .filter(Objects::nonNull)
                .filter(npc -> boss.equalsIgnoreCase(npc.getName()))
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

    private boolean checkKillInterval(int killCount) {
        if (killCount == 1 && config.killCountNotifyInitial())
            return true;

        int interval = config.killCountInterval();
        return interval <= 1 || killCount % interval == 0;
    }

    @VisibleForTesting
    static Optional<Pair<String, Integer>> parse(String message) {
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
