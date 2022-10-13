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
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.deathSendImage(), null);
    }
}
