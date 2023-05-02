package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.notifiers.data.GroupStorageNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetModalMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupStorageNotifierTest extends MockedNotifierTest {

    private static final int OPAL_PRICE = 600;
    private static final int RUBY_PRICE = 900;
    private static final int TUNA_PRICE = 100;

    @Bind
    @InjectMocks
    GroupStorageNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.discordRichEmbeds()).thenReturn(true);
        when(config.notifyBank()).thenReturn(true);
        when(config.bankSendImage()).thenReturn(false);
        when(config.bankNotifyMessage())
            .thenReturn("%USERNAME% has deposited:\n%DEBITS%\n\n%USERNAME% has withdrawn:\n%CREDITS%");

        // init item mocks
        mockItem(ItemID.COINS, 1, "Coins");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.TUNA, TUNA_PRICE, "Tuna");
    }

    @Test
    void testNotify() {
        // mock initial inventory state
        ItemContainer initial = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(initial);
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        when(initial.getItems()).thenReturn(initialItems);

        WidgetLoaded load = new WidgetLoaded();
        load.setGroupId(WidgetID.GROUP_STORAGE_GROUP_ID);
        notifier.onWidgetLoad(load);

        // mock updated inventory
        ItemContainer updated = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(updated);
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        when(updated.getItems()).thenReturn(updatedItems);

        Widget widget = mock(Widget.class);
        when(client.getWidget(GroupStorageNotifier.GROUP_STORAGE_LOADER_ID, 1)).thenReturn(widget);
        when(widget.getText()).thenReturn("Saving...");
        WidgetClosed close = new WidgetClosed(GroupStorageNotifier.GROUP_STORAGE_LOADER_ID, WidgetModalMode.MODAL_NOCLICKTHROUGH, true);
        notifier.onWidgetClose(close);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")),
            Collections.singletonList(new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            RUBY_PRICE - OPAL_PRICE
        );

        String text = String.format(
            "```diff\n%s has deposited:\n%s\n\n%s has withdrawn:\n%s\n```",
            PLAYER_NAME,
            "+ 1 x Ruby (" + RUBY_PRICE + ")",
            PLAYER_NAME,
            "- 1 x Opal (" + OPAL_PRICE + ")"
        );

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(text)
                .extra(extra)
                .type(NotificationType.GROUP_STORAGE)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testNotifyCoins() {
        // mock initial inventory state
        ItemContainer initial = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(initial);
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.COINS_6964, 100) };
        when(initial.getItems()).thenReturn(initialItems);

        WidgetLoaded load = new WidgetLoaded();
        load.setGroupId(WidgetID.GROUP_STORAGE_GROUP_ID);
        notifier.onWidgetLoad(load);

        // mock updated inventory
        ItemContainer updated = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(updated);
        Item[] updatedItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.COINS_8890, 1000) };
        when(updated.getItems()).thenReturn(updatedItems);

        Widget widget = mock(Widget.class);
        when(client.getWidget(GroupStorageNotifier.GROUP_STORAGE_LOADER_ID, 1)).thenReturn(widget);
        when(widget.getText()).thenReturn("Saving...");
        WidgetClosed close = new WidgetClosed(GroupStorageNotifier.GROUP_STORAGE_LOADER_ID, WidgetModalMode.MODAL_NOCLICKTHROUGH, true);
        notifier.onWidgetClose(close);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.emptyList(),
            Collections.singletonList(new SerializedItemStack(ItemID.COINS, 900, 1, "Coins")),
            -900
        );

        String text = String.format(
            "```diff\n%s has deposited:\n%s\n\n%s has withdrawn:\n%s\n```",
            PLAYER_NAME,
            GroupStorageNotifier.EMPTY_TRANSACTION,
            PLAYER_NAME,
            "- 900 x Coins (900)"
        );

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(text)
                .extra(extra)
                .type(NotificationType.GROUP_STORAGE)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testIgnore() {
        // update mocks
        when(config.notifyBank()).thenReturn(false);

        // mock initial inventory state
        ItemContainer initial = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(initial);
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        when(initial.getItems()).thenReturn(initialItems);

        WidgetLoaded load = new WidgetLoaded();
        load.setGroupId(WidgetID.GROUP_STORAGE_GROUP_ID);
        notifier.onWidgetLoad(load);

        // mock updated inventory
        ItemContainer updated = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(updated);
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        when(updated.getItems()).thenReturn(updatedItems);

        WidgetClosed close = new WidgetClosed(GroupStorageNotifier.GROUP_STORAGE_LOADER_ID, WidgetModalMode.MODAL_NOCLICKTHROUGH, true);
        notifier.onWidgetClose(close);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
