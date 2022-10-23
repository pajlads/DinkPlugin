package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;

import javax.inject.Inject;

public class DeathNotifier extends BaseNotifier {

    @Inject
    public DeathNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = plugin.config.deathNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName());
        NotificationBody<Object> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        b.setType(NotificationType.DEATH);
        plugin.messageHandler.createMessage(plugin.config.deathSendImage(), b);
    }
}
