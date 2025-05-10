package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.QuestUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;

public class QuestNotifier extends BaseNotifier {

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
        if (event.getGroupId() == InterfaceID.QUESTSCROLL && isEnabled()) {
            Widget quest = client.getWidget(InterfaceID.Questscroll.QUEST_TITLE);
            if (quest != null) {
                String questText = quest.getText();
                // 1 tick delay to ensure relevant varbits have been processed by the client
                clientThread.invokeLater(() -> handleNotify(questText));
            }
        }
    }

    private void handleNotify(String questText) {
        int completedQuests = client.getVarbitValue(VarbitID.QUESTS_COMPLETED_COUNT);
        int totalQuests = client.getVarbitValue(VarbitID.QUESTS_TOTAL_COUNT);
        boolean validQuests = completedQuests > 0 && totalQuests > 0;

        int questPoints = client.getVarpValue(VarPlayerID.QP);
        int totalQuestPoints = client.getVarbitValue(VarbitID.QP_MAX);
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
