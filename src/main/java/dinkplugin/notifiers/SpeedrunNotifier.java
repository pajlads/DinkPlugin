package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.QuestUtils;
import dinkplugin.util.TimeUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.SpeedrunNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Duration;

@Slf4j
public class SpeedrunNotifier extends BaseNotifier {
    static final @VisibleForTesting int SPEEDRUN_COMPLETED_GROUP_ID = 781;
    static final @VisibleForTesting int SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID = 4;
    static final @VisibleForTesting int SPEEDRUN_COMPLETED_DURATION_CHILD_ID = 10;
    static final @VisibleForTesting int SPEEDRUN_COMPLETED_PB_CHILD_ID = 12;

    @Override
    public boolean isEnabled() {
        return config.notifySpeedrun(); // intentionally doesn't call super
    }

    @Override
    protected String getWebhookUrl() {
        return config.speedrunWebhook();
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == SPEEDRUN_COMPLETED_GROUP_ID && isEnabled()) {
            Widget questName = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_QUEST_NAME_CHILD_ID);
            Widget duration = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_DURATION_CHILD_ID);
            Widget personalBest = client.getWidget(SPEEDRUN_COMPLETED_GROUP_ID, SPEEDRUN_COMPLETED_PB_CHILD_ID);
            if (questName != null && duration != null && personalBest != null) {
                this.attemptNotify(QuestUtils.parseQuestWidget(questName.getText()), duration.getText(), personalBest.getText());
            } else {
                log.error("Found speedrun finished widget (group id {}) but it is missing something, questName={}, duration={}, pb={}", SPEEDRUN_COMPLETED_GROUP_ID, questName, duration, personalBest);
            }
        }
    }

    private void attemptNotify(String questName, String duration, String pb) {
        Duration bestTime = TimeUtils.parseTime(pb);
        Duration currentTime = TimeUtils.parseTime(duration);
        boolean isPb = bestTime.compareTo(currentTime) >= 0;
        if (!isPb && config.speedrunPBOnly()) {
            return;
        }

        // pb or notifying on non-pb; take the right string and format placeholders
        String notifyMessage = StringUtils.replaceEach(
            isPb ? config.speedrunPBMessage() : config.speedrunMessage(),
            new String[] { "%USERNAME%", "%QUEST%", "%TIME%", "%BEST%" },
            new String[] { Utils.getPlayerName(client), questName, duration, pb }
        );

        createMessage(config.speedrunSendImage(), NotificationBody.builder()
            .content(notifyMessage)
            .extra(new SpeedrunNotificationData(questName, bestTime.toString(), currentTime.toString()))
            .type(NotificationType.SPEEDRUN)
            .build());
    }
}
