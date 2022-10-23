package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.CollectionNotificationData;

import javax.inject.Inject;

public class CollectionNotifier extends BaseNotifier {

    @Inject
    public CollectionNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void handleNotify(String itemName) {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = config.collectionNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%ITEM%", itemName);
        NotificationBody<CollectionNotificationData> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        CollectionNotificationData extra = new CollectionNotificationData();
        extra.setItemName(itemName);
        b.setExtra(extra);
        b.setType(NotificationType.COLLECTION);
        messageHandler.createMessage(config.collectionSendImage(), b);
    }
}
