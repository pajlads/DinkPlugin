package dinkplugin;

public class QuestNotifier extends BaseNotifier {

    public QuestNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String questText) {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = plugin.config.questNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%QUEST%", Utils.parseQuestWidget(questText));
        NotificationBody<QuestNotificationData> body = new NotificationBody<>();
        body.setContent(notifyMessage);
        QuestNotificationData extra = new QuestNotificationData();
        extra.setQuestName(Utils.parseQuestWidget(questText));
        body.setExtra(extra);
        body.setType(NotificationType.QUEST);
        plugin.messageHandler.createMessage(plugin.config.questSendImage(), body);
    }
}
