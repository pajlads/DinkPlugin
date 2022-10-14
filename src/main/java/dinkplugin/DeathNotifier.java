package dinkplugin;

import javax.inject.Inject;

public class DeathNotifier extends BaseNotifier {

    @Inject
    public DeathNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        String notifyMessage = plugin.config.deathNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        b.setType(NotificationType.DEATH);
        plugin.messageHandler.createMessage(plugin.config.deathSendImage(), b);
    }
}
