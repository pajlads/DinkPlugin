package universalDiscord;

import javax.inject.Inject;

public class PetNotifier extends BaseNotifier {

    @Inject
    public PetNotifier(UniversalDiscordPlugin plugin) {
        super(plugin);
    }

    @Override
    public void handleNotify() {
        String notifyMessage = plugin.config.petNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName());
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.petSendImage(), null);
    }
}
