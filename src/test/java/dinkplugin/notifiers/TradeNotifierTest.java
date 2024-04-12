package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.notifiers.data.TradeNotificationData;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.List;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TradeNotifierTest extends MockedNotifierTest {
    private static final String COUNTERPARTY = "Billy";

    private static final int OPAL_PRICE = 600;
    private static final int RUBY_PRICE = 900;

    @Bind
    @InjectMocks
    TradeNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // init config mocks
        when(config.notifyTrades()).thenReturn(true);
        when(config.tradeMinValue()).thenReturn(2000);
        when(config.tradeNotifyMessage()).thenReturn("%USERNAME% traded with %COUNTERPARTY%");

        // init item mocks
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
    }

    @Test
    void testNotify() {
        // update mocks
        when(client.getVarcStrValue(TradeNotifier.TRADE_COUNTERPARTY_VAR)).thenReturn(COUNTERPARTY);

        ItemContainer tradeContainer = mock(ItemContainer.class);
        Item[] tradeItems = {new Item(ItemID.OPAL, 2)};
        when(tradeContainer.getItems()).thenReturn(tradeItems);
        when(client.getItemContainer(InventoryID.TRADE)).thenReturn(tradeContainer);

        ItemContainer otherContainer = mock(ItemContainer.class);
        Item[] otherItems = {new Item(ItemID.RUBY, 1)};
        when(otherContainer.getItems()).thenReturn(otherItems);
        when(client.getItemContainer(InventoryID.TRADEOTHER)).thenReturn(otherContainer);

        // fire event
        notifier.onTradeMessage(TradeNotifier.TRADE_ACCEPTED_MESSAGE);

        // verify handled
        List<SerializedItemStack> received = List.of(
            new SerializedItemStack(ItemID.RUBY, 1, RUBY_PRICE, "Ruby")
        );
        List<SerializedItemStack> discarded = List.of(
            new SerializedItemStack(ItemID.OPAL, 2, OPAL_PRICE, "Opal")
        );
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .text(
                    Template.builder()
                        .template("%USERNAME% traded with %COUNTERPARTY%")
                        .replacement("%USERNAME%", Replacements.ofText(PLAYER_NAME))
                        .replacement("%COUNTERPARTY%", Replacements.ofLink(COUNTERPARTY, config.playerLookupService().getPlayerUrl(COUNTERPARTY)))
                        .build()
                )
                .extra(new TradeNotificationData(COUNTERPARTY, received, discarded, RUBY_PRICE, 2 * OPAL_PRICE))
                .type(NotificationType.TRADE)
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Test
    void testIgnoreValue() {
        // update mocks
        when(client.getVarcStrValue(TradeNotifier.TRADE_COUNTERPARTY_VAR)).thenReturn(COUNTERPARTY);

        ItemContainer tradeContainer = mock(ItemContainer.class);
        Item[] tradeItems = {new Item(ItemID.OPAL, 1)};
        when(tradeContainer.getItems()).thenReturn(tradeItems);
        when(client.getItemContainer(InventoryID.TRADE)).thenReturn(tradeContainer);

        ItemContainer otherContainer = mock(ItemContainer.class);
        Item[] otherItems = {new Item(ItemID.RUBY, 1)};
        when(otherContainer.getItems()).thenReturn(otherItems);
        when(client.getItemContainer(InventoryID.TRADEOTHER)).thenReturn(otherContainer);

        // fire event
        notifier.onTradeMessage(TradeNotifier.TRADE_ACCEPTED_MESSAGE);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // update mocks
        when(config.notifyTrades()).thenReturn(false);
        when(client.getVarcStrValue(TradeNotifier.TRADE_COUNTERPARTY_VAR)).thenReturn(COUNTERPARTY);

        ItemContainer tradeContainer = mock(ItemContainer.class);
        Item[] tradeItems = {new Item(ItemID.OPAL, 2)};
        when(tradeContainer.getItems()).thenReturn(tradeItems);
        when(client.getItemContainer(InventoryID.TRADE)).thenReturn(tradeContainer);

        ItemContainer otherContainer = mock(ItemContainer.class);
        Item[] otherItems = {new Item(ItemID.RUBY, 1)};
        when(otherContainer.getItems()).thenReturn(otherItems);
        when(client.getItemContainer(InventoryID.TRADEOTHER)).thenReturn(otherContainer);

        // fire event
        notifier.onTradeMessage(TradeNotifier.TRADE_ACCEPTED_MESSAGE);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

}
