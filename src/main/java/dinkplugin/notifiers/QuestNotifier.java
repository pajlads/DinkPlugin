package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import org.apache.commons.lang3.StringUtils;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;

public class QuestNotifier extends BaseNotifier {

    @Override
    public boolean isEnabled() {
        return config.notifyQuest() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.questWebhook();
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == QUEST_COMPLETED_GROUP_ID && isEnabled()) {
            Widget quest = client.getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT);
            if (quest != null) {
                this.handleNotify(quest.getText());
            }
        }
    }

    private void handleNotify(String questText) {
        String parsed = Utils.parseQuestWidget(questText);
        String notifyMessage = StringUtils.replaceEach(
            config.questNotifyMessage(),
            new String[] { "%USERNAME%", "%QUEST%" },
            new String[] { Utils.getPlayerName(client), parsed }
        );

        createMessage(config.questSendImage(), NotificationBody.builder()
            .content(notifyMessage)
            .extra(new QuestNotificationData(parsed))
            .screenshotFile("questImage.png")
            .type(NotificationType.QUEST)
            .build());
    }
}
