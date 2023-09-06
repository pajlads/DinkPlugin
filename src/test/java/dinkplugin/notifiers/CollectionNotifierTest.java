package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.CollectionNotificationData;
import dinkplugin.util.ItemSearcher;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionNotifierTest extends MockedNotifierTest {

    private static final int TOTAL_ENTRIES = 1443;

    @Bind
    @InjectMocks
    CollectionNotifier notifier;

    @Bind
    @Mock
    ItemSearcher itemSearcher;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyCollectionLog()).thenReturn(true);
        when(config.collectionSendImage()).thenReturn(false);
        when(config.collectionNotifyMessage()).thenReturn("%USERNAME% has added %ITEM% to their collection");

        // init client mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(1);
        when(client.getVarpValue(CollectionNotifier.COMPLETED_VARP)).thenReturn(0);
        when(client.getVarpValue(CollectionNotifier.TOTAL_VARP)).thenReturn(TOTAL_ENTRIES);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        notifier.onTick();
    }

    @Test
    void testNotify() {
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testNotifyPopup() {
        // prepare item
        String item = "Seercull";
        int price = 23_000;
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);
        when(itemManager.getItemPrice(ItemID.SEERCULL)).thenReturn(price);

        // update mocks
        when(client.getVarbitValue(Varbits.COLLECTION_LOG_NOTIFICATION)).thenReturn(3);
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_TOP_TEXT)).thenReturn("Collection log");
        when(client.getVarcStrValue(VarClientStr.NOTIFICATION_BOTTOM_TEXT)).thenReturn("New item:<br>" + item);

        // send chat event (to be ignored)
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification yet
        Mockito.verifyNoInteractions(messageHandler);

        // send script events
        notifier.onScript(ScriptID.NOTIFICATION_START);
        notifier.onScript(ScriptID.NOTIFICATION_DELAY);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template(String.format("%s has added {{item}} to their collection", PLAYER_NAME))
                        .replacement("{{item}}", Replacements.ofWiki(item))
                        .build()
                )
                .extra(new CollectionNotificationData(item, ItemID.SEERCULL, (long) price, 1, TOTAL_ENTRIES))
                .type(NotificationType.COLLECTION)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // send unrelated message
        notifier.onChatMessage("New item added to your backpack: weed");

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // disable notifier
        when(config.notifyCollectionLog()).thenReturn(false);

        // send fake message
        String item = "Seercull";
        notifier.onChatMessage("New item added to your collection log: " + item);

        // ensure no notification occurred
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
