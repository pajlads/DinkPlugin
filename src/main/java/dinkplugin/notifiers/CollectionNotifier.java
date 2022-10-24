package dinkplugin.notifiers;

import dinkplugin.DinkPlugin;
import dinkplugin.DinkPluginConfig;
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

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().notifyCollectionLog() && super.isEnabled();
    }

    public void onChatMessage(String chatMessage) {
        if (!isEnabled()) return;

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
        if (collectionMatcher.find()) {
            this.handleNotify(collectionMatcher.group("itemName"));
        }
    }

    private void handleNotify(String itemName) {
        String notifyMessage = plugin.getConfig().collectionNotifyMessage()
            .replaceAll("%USERNAME%", Utils.getPlayerName(plugin.getClient()))
            .replaceAll("%ITEM%", itemName);

        createMessage(DinkPluginConfig::collectionSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new CollectionNotificationData(itemName))
            .type(NotificationType.COLLECTION)
            .build());
    }
}
