package universalDiscord;

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
        NotificationBody<CollectionNotificationData> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        CollectionNotificationData extra = new CollectionNotificationData();
        extra.setItemName(itemName);
        b.setExtra(extra);

        plugin.messageHandler.createMessage(plugin.config.collectionSendImage(), b);
    }
}
