package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
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

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyQuest() && super.isEnabled();
    }

    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == QUEST_COMPLETED_GROUP_ID && isEnabled()) {
            Widget quest = plugin.getClient().getWidget(WidgetInfo.QUEST_COMPLETED_NAME_TEXT);
            if (quest != null) {
                String questWidget = quest.getText();
                this.handleNotify(questWidget);
            }
        }
    }

    private void handleNotify(String questText) {
        String parsed = Utils.parseQuestWidget(questText);
        String notifyMessage = plugin.getConfig().questNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%QUEST%", parsed);

        createMessage(DinkPluginConfig::questSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new QuestNotificationData(parsed))
            .type(NotificationType.QUEST)
            .build());
    }
}
