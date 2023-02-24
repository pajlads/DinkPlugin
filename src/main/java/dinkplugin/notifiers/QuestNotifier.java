package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.QuestUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;

public class QuestNotifier extends BaseNotifier {

    @Varbit
    @VisibleForTesting
    static final int COMPLETED_ID = 6347, TOTAL_ID = 11877; // https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,questlist_completed%5D.cs2#L5

    @Inject
    private ClientThread clientThread;

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
                String questText = quest.getText();
                clientThread.invokeLater(() -> handleNotify(questText));
            }
        }
    }

    private void handleNotify(String questText) {
        int completed = client.getVarbitValue(COMPLETED_ID);
        int total = client.getVarbitValue(TOTAL_ID);
        boolean validVarbits = completed > 0 && total > 0;

        String parsed = QuestUtils.parseQuestWidget(questText);
        String notifyMessage = StringUtils.replaceEach(
            config.questNotifyMessage(),
            new String[] { "%USERNAME%", "%QUEST%" },
            new String[] { Utils.getPlayerName(client), parsed }
        );

        QuestNotificationData extra = new QuestNotificationData(
            parsed,
            validVarbits ? completed : null,
            validVarbits ? total : null
        );

        createMessage(config.questSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .extra(extra)
            .type(NotificationType.QUEST)
            .build());
    }
}
