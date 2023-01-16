package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.CollectionNotificationData;
import dinkplugin.util.ItemSearcher;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectionNotifierTest extends MockedNotifierTest {

    @Bind
    @InjectMocks
    CollectionNotifier notifier;

    @Bind
    @Spy
    ItemSearcher itemSearcher;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyCollectionLog()).thenReturn(true);
        when(config.collectionSendImage()).thenReturn(false);
        when(config.collectionNotifyMessage()).thenReturn("%USERNAME% has added %ITEM% to their collection");
    }

    @Test
    void testNotify() {
        String item = "Seercull";
        when(itemSearcher.findItemId(item)).thenReturn(ItemID.SEERCULL);

        // send fake message
        notifier.onChatMessage("New item added to your collection log: " + item);

        // verify handled
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(String.format("%s has added %s to their collection", PLAYER_NAME, item))
                .extra(new CollectionNotificationData(item))
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
