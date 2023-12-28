package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.domain.AccountType;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GroupStorageNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Varbits;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Arrays;
import java.util.Collections;

import static dinkplugin.notifiers.GroupStorageNotifier.EMPTY_TRANSACTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupStorageNotifierTest extends MockedNotifierTest {

    private static final String GROUP_NAME = "Dink QA";
    private static final int OPAL_PRICE = 600;
    private static final int RUBY_PRICE = 900;
    private static final int TUNA_PRICE = 100;
    private static final WidgetLoaded LOAD_EVENT;
    private static final WidgetClosed CLOSE_EVENT;

    @Bind
    @InjectMocks
    GroupStorageNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.discordRichEmbeds()).thenReturn(true);
        when(config.notifyGroupStorage()).thenReturn(true);
        when(config.groupStorageSendImage()).thenReturn(false);
        when(config.groupStorageIncludeClan()).thenReturn(true);
        when(config.groupStorageIncludePrice()).thenReturn(true);
        when(config.groupStorageNotifyMessage())
            .thenReturn("%USERNAME% has deposited:\n%DEPOSITED%\n\n%USERNAME% has withdrawn:\n%WITHDRAWN%");

        // init item mocks
        mockItem(ItemID.COINS, 1, "Coins");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.TUNA, TUNA_PRICE, "Tuna");
        mockItem(ItemID.TUNA_26149, TUNA_PRICE, "Tuna");
        when(itemManager.canonicalize(ItemID.TUNA_26149)).thenReturn(ItemID.TUNA);

        // init group mock
        when(client.getVarbitValue(Varbits.ACCOUNT_TYPE)).thenReturn(AccountType.HARDCORE_GROUP_IRONMAN.ordinal());
        ClanChannel channel = mock(ClanChannel.class);
        when(channel.getName()).thenReturn(GROUP_NAME);
        when(client.getClanChannel(ClanID.GROUP_IRONMAN)).thenReturn(channel);
    }

    @Test
    void testNotify() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")),
            Collections.singletonList(new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            RUBY_PRICE - OPAL_PRICE,
            GROUP_NAME,
            true
        );

        verifyNotification(extra, "+ 1 x Ruby (" + RUBY_PRICE + ")", "- 1 x Opal (" + OPAL_PRICE + ")");
    }

    @Test
    void testNotifyNonePrice() {
        when(config.groupStorageIncludePrice()).thenReturn(false);

        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 2), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 2, RUBY_PRICE, "Ruby")),
            Collections.singletonList(new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            (2*RUBY_PRICE) - OPAL_PRICE,
            GROUP_NAME,
            false
        );

        verifyNotification(extra, "+ 2 x Ruby", "- 1 x Opal");
    }

    @Test
    void testNotifyCoins() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.COINS_6964, 100) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.COINS_8890, 1000) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.emptyList(),
            Collections.singletonList(new SerializedItemStack(ItemID.COINS, 900, 1, "Coins")),
            -900,
            GROUP_NAME,
            true
        );

        verifyNotification(extra, EMPTY_TRANSACTION, "- 900 x Coins (900)");
    }

    @Test
    void testNotifyStackable() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 2), new Item(ItemID.COINS_6964, 100), new Item(ItemID.COINS_995, 150) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.RUBY, 1), new Item(ItemID.COINS_8890, 1000), new Item(ItemID.COINS, 50) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.emptyList(),
            Collections.singletonList(new SerializedItemStack(ItemID.COINS, 800, 1, "Coins")),
            -800,
            GROUP_NAME,
            true
        );

        verifyNotification(extra, EMPTY_TRANSACTION, "- 800 x Coins (800)");
    }

    @Test
    void testNotifyMultipleDebit() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        mockContainer(new Item[0]);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            Collections.emptyList(),
            RUBY_PRICE + OPAL_PRICE,
            GROUP_NAME,
            true
        );

        verifyNotification(extra, "+ 1 x Ruby (" + RUBY_PRICE + ")\n+ 1 x Opal (" + OPAL_PRICE + ")", EMPTY_TRANSACTION);
    }

    @Test
    void testNotifyMultipleCredit() {
        // mock initial inventory state
        mockContainer(new Item[0]);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.emptyList(),
            Arrays.asList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby"), new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            -(RUBY_PRICE + OPAL_PRICE),
            GROUP_NAME,
            true
        );

        verifyNotification(extra, EMPTY_TRANSACTION, "- 1 x Ruby (" + RUBY_PRICE + ")\n- 1 x Opal (" + OPAL_PRICE + ")");
    }

    @Test
    void testNotifyNoted() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.TUNA, 2) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA_26149, 3) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.emptyList(),
            Collections.singletonList(new SerializedItemStack(ItemID.TUNA, 1, TUNA_PRICE, "Tuna")),
            -TUNA_PRICE,
            GROUP_NAME,
            true
        );

        verifyNotification(extra, EMPTY_TRANSACTION, "- 1 x Tuna (" + TUNA_PRICE + ")");
    }

    @Test
    void testWithoutGroupName() {
        // update config mocks
        when(config.groupStorageIncludeClan()).thenReturn(false);

        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // verify notification message
        GroupStorageNotificationData extra = new GroupStorageNotificationData(
            Collections.singletonList(new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")),
            Collections.singletonList(new SerializedItemStack(ItemID.OPAL, 1, OPAL_PRICE, "Opal")),
            RUBY_PRICE - OPAL_PRICE,
            null,
            true
        );

        verifyNotification(extra, "+ 1 x Ruby (" + RUBY_PRICE + ")", "- 1 x Opal (" + OPAL_PRICE + ")");
    }

    @Test
    void testIgnoreNoted() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.TUNA, 2) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA_26149, 2) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreValue() {
        // update config mocks
        when(config.groupStorageMinValue()).thenReturn(RUBY_PRICE + 1);

        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreZero() {
        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.RUBY, 1) };
        mockContainer(updatedItems);

        mockSaveWidget();
        notifier.onWidgetClose(CLOSE_EVENT);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnore() {
        // update mocks
        when(config.notifyGroupStorage()).thenReturn(false);

        // mock initial inventory state
        Item[] initialItems = { new Item(ItemID.RUBY, 1), new Item(ItemID.TUNA, 1) };
        mockContainer(initialItems);
        notifier.onWidgetLoad(LOAD_EVENT);

        // mock updated inventory
        Item[] updatedItems = { new Item(ItemID.TUNA, 1), new Item(ItemID.OPAL, 1) };
        mockContainer(updatedItems);
        notifier.onWidgetClose(CLOSE_EVENT);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private void verifyNotification(GroupStorageNotificationData extra, String deposited, String withdrawn) {
        String text = String.format(
            "%s has deposited:\n%s\n\n%s has withdrawn:\n%s",
            PLAYER_NAME, deposited, PLAYER_NAME, withdrawn
        );

        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template("{{x}}")
                        .replacement("{{x}}", Replacements.ofBlock("diff", text))
                        .build()
                )
                .extra(extra)
                .type(NotificationType.GROUP_STORAGE)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    private void mockContainer(Item[] items) {
        ItemContainer container = mock(ItemContainer.class);
        when(container.getItems()).thenReturn(items);
        when(client.getItemContainer(InventoryID.GROUP_STORAGE_INV)).thenReturn(container);
    }

    private void mockSaveWidget() {
        Widget widget = mock(Widget.class);
        when(client.getWidget(GroupStorageNotifier.GROUP_STORAGE_SAVING_WIDGET_ID, 1)).thenReturn(widget);
        when(widget.getText()).thenReturn("Saving...");
    }

    static {
        LOAD_EVENT = new WidgetLoaded();
        LOAD_EVENT.setGroupId(GroupStorageNotifier.GROUP_STORAGE_WIDGET_GROUP);
        CLOSE_EVENT = new WidgetClosed(GroupStorageNotifier.GROUP_STORAGE_SAVING_WIDGET_ID, WidgetModalMode.MODAL_NOCLICKTHROUGH, true);
    }
}
