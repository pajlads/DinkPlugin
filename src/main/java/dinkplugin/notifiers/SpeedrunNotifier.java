package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.SpeedrunNotificationData;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeedrunNotifier extends BaseNotifier {
    public SpeedrunNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void attemptNotify(String questName, String duration, String pb) {
        Duration bestTime = this.parseTime(pb);
        Duration currentTime = this.parseTime(duration);
        boolean isPb = bestTime.compareTo(currentTime) >= 0;
        if (!isPb && plugin.config.speedrunPBOnly()) {
            return;
        }
        // pb or notifying on non-pb; take the right string and format placeholders
        String notifyMessage = plugin.config.speedrunMessage();
        if (isPb) {
            notifyMessage = plugin.config.speedrunPBMessage();
        }

        notifyMessage = notifyMessage
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%QUEST%", questName)
            .replaceAll("%TIME%", duration)
            .replaceAll("%BEST%", pb);
        NotificationBody<SpeedrunNotificationData> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        SpeedrunNotificationData extra = new SpeedrunNotificationData();
        extra.setQuestName(questName);
        extra.setPersonalBest(bestTime.toString());
        extra.setCurrentTime(currentTime.toString());
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
