package universalDiscord;

import javax.inject.Inject;

public class CollectionNotifier {
    private final UniversalDiscordPlugin plugin;

    @Inject
    public CollectionNotifier(UniversalDiscordPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleNotify(String message) {
        String itemName = message.substring(38);
        String notifyMessage = plugin.config.collectionNotifyMessage().replaceAll("%USERNAME", Utils.getPlayerName()).replaceAll("%ITEM%", itemName);
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.collectionSendImage(), null);
    }
}
