package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.Utils;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class KillCountNotifier extends BaseNotifier {
    private static final Pattern KC_REGEX = Pattern.compile("Your (?<boss>.+) kill count is: (?<kc>\\d+)\\b");

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
        if (killCount == 1) return true; // always notify on first boss kill
        int interval = plugin.getConfig().killCountInterval();
        return interval <= 1 || killCount % interval == 0;
    }

    static Optional<Pair<String, Integer>> parse(String message) {
        Matcher matcher = KC_REGEX.matcher(message);
        if (matcher.find()) {
            String boss = matcher.group("boss");
            String count = matcher.group("kc");
            try {
                return Optional.of(Pair.of(boss, Integer.parseInt(count)));
            } catch (NumberFormatException e) {
                log.debug("Failed to parse kill count from message: {}", message);
            }
        }
        return Optional.empty();
    }
}
