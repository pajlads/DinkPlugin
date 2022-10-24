package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.SpeedrunNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SpeedrunNotifier extends BaseNotifier {
    private static final Pattern TIME_PATTERN = Pattern.compile("(?<minutes>\\d+):(?<seconds>\\d{2})\\.(?<fractional>\\d{2})");
    private static final int SPEEDRUN_COMPLETED_GROUP_ID = 781;
    private static final int SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID = 4;
    private static final int SPEEDRUN_COMPLETED_DURATION_CHILD_ID = 10;
    private static final int SPEEDRUN_COMPLETED_PB_CHILD_ID = 12;

    public SpeedrunNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifySpeedrun(); // intentionally doesn't call super
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == SPEEDRUN_COMPLETED_GROUP_ID && isEnabled()) {
            Client client = plugin.getClient();
            Widget questName = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID);
            Widget duration = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID);
            Widget personalBest = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_PB_CHILD_ID);
            if (questName != null && duration != null && personalBest != null) {
                this.attemptNotify(Utils.parseQuestWidget(questName.getText()), duration.getText(), personalBest.getText());
            } else {
                log.error("Found speedrun finished widget (group id {}) but it is missing something, questName={}, duration={}, pb={}", SPEEDRUN_COMPLETED_GROUP_ID, questName, duration, personalBest);
            }
        }
    }

    private void attemptNotify(String questName, String duration, String pb) {
        Duration bestTime = parseTime(pb);
        Duration currentTime = parseTime(duration);
        boolean isPb = bestTime.compareTo(currentTime) >= 0;
        if (!isPb && plugin.getConfig().speedrunPBOnly()) {
            return;
        }

        // pb or notifying on non-pb; take the right string and format placeholders
        String pattern = isPb ? plugin.getConfig().speedrunPBMessage() : plugin.getConfig().speedrunMessage();
        String notifyMessage = pattern
            .replaceAll("%USERNAME%", Utils.getPlayerName(plugin.getClient()))
            .replaceAll("%QUEST%", questName)
            .replaceAll("%TIME%", duration)
            .replaceAll("%BEST%", pb);

        createMessage(DinkPluginConfig::speedrunSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new SpeedrunNotificationData(questName, bestTime.toString(), currentTime.toString()))
            .type(NotificationType.SPEEDRUN)
            .build());
    }

    private static Duration parseTime(String in) {
        Matcher m = TIME_PATTERN.matcher(in);
        if (!m.find()) return Duration.ofMillis(0);
        int minutes = Integer.parseInt(m.group("minutes"));
        int seconds = Integer.parseInt(m.group("seconds"));
        int fractional = Integer.parseInt(m.group("fractional"));
        // 0.001
        return Duration.ofMillis(10L * (100L * (60L * minutes + seconds) + fractional));
    }
}
