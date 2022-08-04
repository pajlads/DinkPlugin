package universalDiscord;

import javax.inject.Inject;

public class PetNotifier {
    private final UniversalDiscordPlugin plugin;

    @Inject
    public PetNotifier(UniversalDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify(String message) {
        String notifyMessage = plugin.config.collectionNotifyMessage().replaceAll("%USERNAME", Utils.getPlayerName());
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.petSendImage(), null);
    }
}
