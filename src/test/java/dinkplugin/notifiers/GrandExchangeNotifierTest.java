package dinkplugin.notifiers;

import com.google.inject.testing.fieldbinder.Bind;
import dinkplugin.message.Embed;
import dinkplugin.message.NotificationBody;
import dinkplugin.message.NotificationType;
import dinkplugin.message.templating.Replacements;
import dinkplugin.message.templating.Template;
import dinkplugin.notifiers.data.GrandExchangeNotificationData;
import dinkplugin.notifiers.data.SerializedItemStack;
import dinkplugin.util.ItemUtils;
import lombok.Value;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemID;
import net.runelite.client.util.QuantityFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class GrandExchangeNotifierTest extends MockedNotifierTest {

    private static final int RUBY_PRICE = 900;
    private static final int OPAL_PRICE = 600;

    @Bind
    @InjectMocks
    GrandExchangeNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() {
        super.setUp();

        // config mocks
        when(config.notifyGrandExchange()).thenReturn(true);
        when(config.grandExchangeMinValue()).thenReturn(5000);
        when(config.grandExchangeProgressSpacingMinutes()).thenReturn(-1);
        when(config.grandExchangeNotifyMessage()).thenReturn("%USERNAME% %TYPE% %ITEM% on the GE");

        // item mocks
        mockItem(ItemID.RUBY, RUBY_PRICE, "Ruby");
        mockItem(ItemID.OPAL, OPAL_PRICE, "Opal");
    }

    @Test
    void testNotifyBuy() {
        // fire event
        Offer offer = new Offer(10, ItemID.RUBY, 10, RUBY_PRICE, 10_000, GrandExchangeOfferState.BOUGHT);
        notifier.onOfferChange(0, offer);

        // verify notification
        verifyNotification(0, offer, "bought", "Ruby", RUBY_PRICE, null);
    }

    @Test
    void testNotifySell() {
        // fire event
        Offer offer = new Offer(10, ItemID.OPAL, 10, OPAL_PRICE, 7_000, GrandExchangeOfferState.SOLD);
        notifier.onOfferChange(1, offer);

        // verify notification
        verifyNotification(1, offer, "sold", "Opal", OPAL_PRICE, 10 * 7L);
    }

    @Test
    void testIgnoreValue() {
        // fire event
        Offer offer = new Offer(10, ItemID.OPAL, 5, OPAL_PRICE, 3_500, GrandExchangeOfferState.SOLD);
        notifier.onOfferChange(1, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreBuying() {
        // fire event
        Offer offer = new Offer(10, ItemID.RUBY, 50, RUBY_PRICE, 10_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(0, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreSelling() {
        // fire event
        Offer offer = new Offer(10, ItemID.OPAL, 50, OPAL_PRICE, 3_500, GrandExchangeOfferState.SELLING);
        notifier.onOfferChange(1, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testNotifyPartialDifferentSlots() {
        // update config mock
        when(config.grandExchangeProgressSpacingMinutes()).thenReturn(0);

        // fire event
        Offer offer = new Offer(11, ItemID.RUBY, 50, RUBY_PRICE, 11_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(1, offer);

        // verify notification
        verifyNotification(1, offer, "bought", "Ruby", RUBY_PRICE, null);

        // fire second event without spacing
        Offer offer2 = new Offer(22, ItemID.OPAL, 60, OPAL_PRICE, 15_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(2, offer2);

        // verify second notification
        verifyNotification(2, offer2, "bought", "Opal", OPAL_PRICE, null);
    }

    @Test
    void testNotifySpacing() throws InterruptedException {
        // update config mock
        when(config.grandExchangeProgressSpacingMinutes()).thenReturn(0);

        // fire event
        Offer offer = new Offer(11, ItemID.RUBY, 50, RUBY_PRICE, 11_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(0, offer);

        // verify notification
        verifyNotification(0, offer, "bought", "Ruby", RUBY_PRICE, null);

        // allow time to pass
        Thread.sleep(2500);

        // fire second event
        Offer offer2 = new Offer(22, ItemID.RUBY, 50, RUBY_PRICE, 22_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(0, offer2);

        // verify second notification
        verifyNotification(0, offer2, "bought", "Ruby", RUBY_PRICE, null);
    }

    @Test
    void testIgnoreSpacing() {
        // update config mock
        when(config.grandExchangeProgressSpacingMinutes()).thenReturn(0);

        // fire event
        Offer offer = new Offer(10, ItemID.RUBY, 50, RUBY_PRICE, 10_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(0, offer);

        // verify notification
        verifyNotification(0, offer, "bought", "Ruby", RUBY_PRICE, null);

        // fire second event without spacing
        Offer offer2 = new Offer(20, ItemID.RUBY, 50, RUBY_PRICE, 10_000, GrandExchangeOfferState.BUYING);
        notifier.onOfferChange(0, offer2);

        // ensure no notification for second event
        verifyNoMoreInteractions(messageHandler);
    }

    @Test
    void testNotifyCancelled() {
        // update config mock
        when(config.grandExchangeIncludeCancelled()).thenReturn(true);

        // fire event
        Offer offer = new Offer(11, ItemID.RUBY, 50, RUBY_PRICE, 11_000, GrandExchangeOfferState.CANCELLED_BUY);
        notifier.onOfferChange(0, offer);

        // verify notification
        verifyNotification(0, offer, "bought", "Ruby", RUBY_PRICE, null);
    }

    @Test
    void testIgnoreCancelledValue() {
        // update config mock
        when(config.grandExchangeIncludeCancelled()).thenReturn(true);

        // fire event
        Offer offer = new Offer(2, ItemID.RUBY, 50, RUBY_PRICE, 2_000, GrandExchangeOfferState.CANCELLED_BUY);
        notifier.onOfferChange(0, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testIgnoreCancelled() {
        // fire event
        Offer offer = new Offer(11, ItemID.RUBY, 50, RUBY_PRICE, 11_000, GrandExchangeOfferState.CANCELLED_BUY);
        notifier.onOfferChange(0, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    @Test
    void testDisabled() {
        // update config mock
        when(config.notifyGrandExchange()).thenReturn(false);

        // fire event
        Offer offer = new Offer(10, ItemID.RUBY, 10, RUBY_PRICE, 10_000, GrandExchangeOfferState.BOUGHT);
        notifier.onOfferChange(0, offer);

        // ensure no notification
        verify(messageHandler, never()).createMessage(any(), anyBoolean(), any());
    }

    private void verifyNotification(int slot, Offer offer, String type, String itemName, long marketPrice, Long tax) {
        SerializedItemStack item = new SerializedItemStack(offer.getItemId(), offer.getQuantitySold(), offer.getSpent() / offer.getQuantitySold(), itemName);
        verify(messageHandler).createMessage(
            PRIMARY_WEBHOOK_URL,
            false,
            NotificationBody.builder()
                .type(NotificationType.GRAND_EXCHANGE)
                .text(
                    Template.builder()
                        .template(PLAYER_NAME + " {{type}} {{quantity}} x {{item}} ({{value}}) on the GE")
                        .replacement("{{type}}", Replacements.ofText(type))
                        .replacement("{{quantity}}", Replacements.ofText(String.valueOf(offer.getQuantitySold())))
                        .replacement("{{item}}", Replacements.ofWiki(itemName))
                        .replacement("{{value}}", Replacements.ofText(QuantityFormatter.quantityToStackSize(item.getTotalPrice())))
                        .build()
                )
                .embeds(Collections.singletonList(Embed.ofImage(ItemUtils.getItemImageUrl(item.getId()))))
                .extra(new GrandExchangeNotificationData(slot + 1, offer.getState(), item, marketPrice, offer.getPrice(), offer.getTotalQuantity(), tax))
                .playerName(PLAYER_NAME)
                .build()
        );
    }

    @Value
    private static class Offer implements GrandExchangeOffer {
        int quantitySold;
        int itemId;
        int totalQuantity;
        int price;
        int spent;
        GrandExchangeOfferState state;
    }
}
