package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.QuestUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.VarPlayer;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;

public class QuestNotifier extends BaseNotifier {

    /*
     * https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,questlist_completed%5D.cs2#L5
     */
    @Varbit
    public static final int COMPLETED_ID = 6347, TOTAL_ID = 11877;

    /*
     * https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,questlist_qp%5D.cs2#L5
     */
    @Varbit
    @VisibleForTesting
    static final int QP_TOTAL_ID = 1782;

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
        if (event.getGroupId() == InterfaceID.QUEST_COMPLETED && isEnabled()) {
            Widget quest = client.getWidget(ComponentID.QUEST_COMPLETED_NAME_TEXT);
            if (quest != null) {
                String questText = quest.getText();
                // 1 tick delay to ensure relevant varbits have been processed by the client
                clientThread.invokeLater(() -> handleNotify(questText));
            }
        }
    }

    private void handleNotify(String questText) {
        int completedQuests = client.getVarbitValue(COMPLETED_ID);
        int totalQuests = client.getVarbitValue(TOTAL_ID);
        boolean validQuests = completedQuests > 0 && totalQuests > 0;

        int questPoints = client.getVarpValue(VarPlayer.QUEST_POINTS);
        int totalQuestPoints = client.getVarbitValue(QP_TOTAL_ID);
        boolean validPoints = questPoints > 0 && totalQuestPoints > 0;

        String parsed = QuestUtils.parseQuestWidget(questText);
        if (parsed == null) return;

        Template notifyMessage = Template.builder()
            .template(config.questNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%QUEST%", Replacements.ofWiki(parsed))
            .build();

        QuestNotificationData extra = new QuestNotificationData(
            parsed,
            validQuests ? completedQuests : null,
            validQuests ? totalQuests : null,
            validPoints ? questPoints : null,
            validPoints ? totalQuestPoints : null
        );

        createMessage(config.questSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .extra(extra)
            .type(NotificationType.QUEST)
            .build());
    }
}
