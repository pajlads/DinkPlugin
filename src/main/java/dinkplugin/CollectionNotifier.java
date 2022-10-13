package dinkplugin;

import javax.inject.Inject;

public class CollectionNotifier extends BaseNotifier {

    @Inject
    public CollectionNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String itemName) {
        String notifyMessage = plugin.config.collectionNotifyMessage()
                .replaceAll("%USERNAME%", Utils.getPlayerName())
                .replaceAll("%ITEM%", itemName);
        plugin.messageHandler.createMessage(notifyMessage, plugin.config.collectionSendImage(), null);
    }
}
