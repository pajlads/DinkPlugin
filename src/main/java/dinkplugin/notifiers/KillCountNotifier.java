package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class KillCountNotifier extends BaseNotifier {
    private static final Pattern PRIMARY_REGEX = Pattern.compile("Your (?<key>.+)\\s(?<type>kill|chest|completion)\\s?count is: (?<value>\\d+)\\b");
    private static final Pattern SECONDARY_REGEX = Pattern.compile("Your (?:completed|subdued) (?<key>.+) count is: (?<value>\\d+)\\b");

    @Inject
    public KillCountNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyKillCount() && super.isEnabled();
    }

    public void onGameMessage(String message) {
        if (isEnabled())
            parse(message).ifPresent(pair -> handleKill(pair.getLeft(), pair.getRight()));
    }

    private void handleKill(String boss, int killCount) {
        if (!checkKillInterval(killCount)) return;

        String player = Utils.getPlayerName(plugin.getClient());
        String content = StringUtils.replaceEach(
            plugin.getConfig().killCountMessage(),
            new String[] { "%USERNAME%", "%BOSS%", "%COUNT%" },
            new String[] { player, boss, String.valueOf(killCount) }
        );

        createMessage(DinkPluginConfig::killCountSendImage, NotificationBody.builder()
            .content(content)
            .playerName(player)
            .type(NotificationType.KILL_COUNT)
            .build());
    }

    private boolean checkKillInterval(int killCount) {
        if (killCount == 1 && plugin.getConfig().killCountNotifyInitial())
            return true;

        int interval = plugin.getConfig().killCountInterval();
        return interval <= 1 || killCount % interval == 0;
    }

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
