package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.QuestNotificationData;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import static net.runelite.api.widgets.WidgetID.QUEST_COMPLETED_GROUP_ID;

public class QuestNotifier extends BaseNotifier {

    public QuestNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == QUEST_COMPLETED_GROUP_ID && plugin.getConfig().notifyQuest()) {
            Widget quest = plugin.getClient().getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT);
            if (quest != null) {
                String questWidget = quest.getText();
                this.handleNotify(questWidget);
            }
        }
    }

    private void handleNotify(String questText) {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = plugin.getConfig().questNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%QUEST%", Utils.parseQuestWidget(questText));
        NotificationBody<QuestNotificationData> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        QuestNotificationData extra = new QuestNotificationData();
        extra.setQuestName(Utils.parseQuestWidget(questText));
        body.setExtra(extra);
        body.setType(NotificationType.QUEST);
        messageHandler.createMessage(plugin.getConfig().questSendImage(), body);
    }
}
