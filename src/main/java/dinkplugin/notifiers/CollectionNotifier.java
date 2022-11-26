package dinkplugin.notifiers;

import dinkplugin.DinkPluginConfig;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.Utils;
import dinkplugin.notifiers.data.CollectionNotificationData;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionNotifier extends BaseNotifier {
    @VisibleForTesting
    static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>(.*))");

    @Override
    public boolean isEnabled() {
        return config.notifyCollectionLog() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.collectionWebhook();
    }

    public void onChatMessage(String chatMessage) {
        if (!isEnabled()) return;

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
        if (collectionMatcher.find()) {
            this.handleNotify(collectionMatcher.group("itemName"));
        }
    }

    private void handleNotify(String itemName) {
        String notifyMessage = StringUtils.replaceEach(
            config.collectionNotifyMessage(),
            new String[] { "%USERNAME%", "%ITEM%" },
            new String[] { Utils.getPlayerName(client), itemName }
        );

        createMessage(DinkPluginConfig::collectionSendImage, NotificationBody.builder()
            .content(notifyMessage)
            .extra(new CollectionNotificationData(itemName))
            .type(NotificationType.COLLECTION)
            .build());
    }
}
