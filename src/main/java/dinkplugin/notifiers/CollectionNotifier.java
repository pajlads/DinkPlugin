package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.CollectionNotificationData;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.game.ItemManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionNotifier extends BaseNotifier {
    static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>(.*))");
    public static final String ADDITION_WARNING = "Collection notifier will not fire unless you enable the game setting: Collection log - New addition notification";

    @VisibleForTesting
    static final int COMPLETED_VARP = 2943, TOTAL_VARP = 2944; // https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/[clientscript,collection_init_frame].cs2#L3

    /**
     * The number of completed entries in the collection log, as implied by {@link #COMPLETED_VARP}.
     */
    private final AtomicInteger completed = new AtomicInteger(-1);

    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ItemSearcher itemSearcher;

    @Override
    public boolean isEnabled() {
        return config.notifyCollectionLog() && super.isEnabled();
    }

    @Override
    protected String getWebhookUrl() {
        return config.collectionWebhook();
    }

    public void reset() {
        // note: unlike other notifiers, we do not need to reset completed after each message
        this.completed.set(-1);
    }

    public void onTick() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            // indicate that the latest completion count should be updated
            completed.set(-1);
        } else if (completed.get() < 0) {
            // initialize collection log entry completion count
            completed.set(client.getVarpValue(COMPLETED_VARP));
        }
    }

    public void onVarPlayer(VarbitChanged event) {
        if (event.getVarpId() != COMPLETED_VARP)
            return;

        // Currently, this varp is sent early enough to be read on the first logged-in tick.
        // For robustness, we also allow initialization here just in case the varp is sent with greater delay.

        // Also, it is worth noting that this varp is not updated until a few ticks after the collection log message.
        // However, this behavior could also change, which is why here we don't synchronize "completed" beyond initialization.

        int old = completed.get();
        if (old <= 0) {
            completed.compareAndSet(old, event.getValue());
        }
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

        // varp isn't updated for a few ticks, so we increment the count locally.
        // this approach also has the benefit of yielding incrementing values even when
        // multiple collection log entries are completed within a single tick.
        int completed = this.completed.incrementAndGet();
        int total = client.getVarpValue(TOTAL_VARP); // unique; doesn't over-count duplicates
        boolean varpValid = total > 0 && completed >= 0;

        Integer itemId = itemSearcher.findItemId(itemName);
        Long price = itemId != null ? ItemUtils.getPrice(itemManager, itemId) : null;
        CollectionNotificationData extra = new CollectionNotificationData(
            itemName,
            itemId,
            price,
            varpValid ? completed : null,
            varpValid ? total : null
        );

        createMessage(config.collectionSendImage(), NotificationBody.builder()
            .text(notifyMessage)
            .thumbnailUrl(itemId != null ? ItemUtils.getItemImageUrl(itemId) : null)
            .extra(extra)
            .type(NotificationType.COLLECTION)
            .build());
    }
}
