package dinkplugin.notifiers;

import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.util.ItemSearcher;
import dinkplugin.util.ItemUtils;
import dinkplugin.util.Utils;
import dinkplugin.notifiers.data.CollectionNotificationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.annotations.Varp;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class CollectionNotifier extends BaseNotifier {
    static final Pattern COLLECTION_LOG_REGEX = Pattern.compile("New item added to your collection log: (?<itemName>(.*))");
    public static final String ADDITION_WARNING = "Collection notifier will not fire unless you enable the game setting: Collection log - New addition notification";
    private static final int POPUP_PREFIX_LENGTH = "New item:".length();

    /*
     * https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bclientscript,collection_init_frame%5D.cs2#L3
     */
    public static final @Varp int COMPLETED_VARP = 2943, TOTAL_VARP = 2944;

    /**
     * The number of completed entries in the collection log, as implied by {@link #COMPLETED_VARP}.
     */
    private final AtomicInteger completed = new AtomicInteger(-1);

    private final AtomicBoolean popupStarted = new AtomicBoolean(false);

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

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
        // in fact, resetting would be problematic for an edge case with multiple completions in a single tick
        this.completed.set(-1);
        this.popupStarted.set(false);
    }

    public void onGameState(GameState newState) {
        if (newState != GameState.HOPPING && newState != GameState.LOGGED_IN)
            this.reset();
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

        // Note: upon a completion, this varp is not updated until a few ticks after the collection log message.
        // However, this behavior could also change, which is why here we don't synchronize "completed" beyond initialization.

        int old = completed.get();
        if (old <= 0) {
            completed.compareAndSet(old, event.getValue());
        }
    }

    public void onChatMessage(String chatMessage) {
        if (!isEnabled() || client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION) != 1) {
            // require notifier enabled without popup mode to use chat event
            return;
        }

        Matcher collectionMatcher = COLLECTION_LOG_REGEX.matcher(chatMessage);
        if (collectionMatcher.find()) {
            String item = collectionMatcher.group("itemName");
            clientThread.invokeLater(() -> handleNotify(item));
        }
    }

    public void onScript(int scriptId) {
        if (scriptId == ScriptID.NOTIFICATION_START) {
            popupStarted.set(true);
        } else if (scriptId == ScriptID.NOTIFICATION_DELAY) {
            String topText = client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT);
            if (popupStarted.getAndSet(false) && "Collection log".equalsIgnoreCase(topText) && isEnabled()) {
                String bottomText = Utils.sanitize(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT));
                handleNotify(bottomText.substring(POPUP_PREFIX_LENGTH).trim());
            }
        }
    }

    private void handleNotify(String itemName) {
        Template notifyMessage = Template.builder()
            .template(config.collectionNotifyMessage())
            .replacementBoundary("%")
            .replacement("%USERNAME%", Replacements.ofText(Utils.getPlayerName(client)))
            .replacement("%ITEM%", Replacements.ofWiki(itemName))
            .build();

        // varp isn't updated for a few ticks, so we increment the count locally.
        // this approach also has the benefit of yielding incrementing values even when
        // multiple collection log entries are completed within a single tick.
        int completed = this.completed.incrementAndGet();
        int total = client.getVarpValue(TOTAL_VARP); // unique; doesn't over-count duplicates
        boolean varpValid = total > 0 && completed > 0;
        if (!varpValid) {
            // This occurs if the player doesn't have the character summary tab selected
            log.debug("Collection log progress varps were invalid ({} / {})", completed, total);
        }

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
