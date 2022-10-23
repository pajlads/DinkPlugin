package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.CollectionNotificationData;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionNotifier extends BaseNotifier {
    static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>(.*))");

    @Inject
    public CollectionNotifier(DinkPlugin plugin) {
        super(plugin);
    }

    public void onChatMessage(String chatMessage) {
        if (!plugin.getConfig().notifyCollectionLog()) return;

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
        if (collectionMatcher.find()) {
            this.handleNotify(collectionMatcher.group("itemName"));
        }
    }

    private void handleNotify(String itemName) {
        if (plugin.isIgnoredWorld()) return;
        String notifyMessage = plugin.getConfig().collectionNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName())
            .replaceAll("%ITEM%", itemName);
        NotificationBody<CollectionNotificationData> b = new NotificationBody<>();
        b.setContent(notifyMessage);
        CollectionNotificationData extra = new CollectionNotificationData();
        extra.setItemName(itemName);
        b.setExtra(extra);
        b.setType(NotificationType.COLLECTION);
        messageHandler.createMessage(plugin.getConfig().collectionSendImage(), b);
    }
}
