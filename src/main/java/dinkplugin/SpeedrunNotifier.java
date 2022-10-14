package dinkplugin;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeedrunNotifier extends BaseNotifier {
    public SpeedrunNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public boolean cheat = true;

    public void handleNotify(String questName, String pb) {
        Duration bestTime = this.parseTime(pb);
        String notifyMessage = plugin.config.speedrunPBMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%QUEST%", questName)
            .replaceAll("%TIME%", pb);
        NotificationBody<SpeedrunPBNotificationData> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        SpeedrunPBNotificationData extra = new SpeedrunPBNotificationData();
        extra.setQuestName(questName);
        extra.setPersonalBest(bestTime);
        body.setExtra(extra);
        body.setType(NotificationType.SPEEDRUN);
        plugin.messageHandler.createMessage(plugin.config.speedrunSendImage(), body);
    }

    private static final Pattern TIME_PATTERN = Pattern.compile("(?<minutes>\\d+):(?<seconds>\\d{2})\\.(?<fractional>\\d{2})");

    public Duration parseTime(String in) {
        Matcher m = TIME_PATTERN.matcher(in);
        if (!m.find()) return Duration.ofMillis(0);
        int minutes = Integer.parseInt(m.group("minutes"));
        int seconds = Integer.parseInt(m.group("seconds"));
        int fractional = Integer.parseInt(m.group("fractional"));
        // 0.001
        return Duration.ofMillis(10L * (100L * (60L * minutes + seconds) + fractional));
    }
}
