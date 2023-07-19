package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.notifiers.GrandExchangeNotifier;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.GrandExchangeOfferState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class GrandExchangeNotificationData extends NotificationData {

    /**
     * The slot index (one-based) for this transaction.
     * This ranges from 1 to 8 (inclusive) for members, and 1 to 3 (inclusive) for F2P.
     */
    int slot;

    /**
     * The trade offer status.
     * Will not be {@link GrandExchangeOfferState#EMPTY}.
     */
    @NotNull
    GrandExchangeOfferState status;

    /**
     * The transacted item, including the number of items transacted and the pre-tax average price.
     */
    @NotNull
    SerializedItemStack item;

    /**
     * The current market price for the transacted item, according to wiki data.
     * This can differ from the transacted price contained in {@link #getItem()}.
     */
    long marketPrice;

    /**
     * The limit order price selected by the user for the offer.
     * For sell (buy) offers, this is a lower (upper) bound on the price contained in {@link #getItem()}.
     */
    long targetPrice;

    /**
     * The total number of items that the player wishes to buy within this slot.
     * When {@link #getStatus()} is {@link GrandExchangeOfferState#BOUGHT} or {@link GrandExchangeOfferState#SOLD},
     * this quantity is equivalent to the quantity contained within {@link #getItem()}.
     */
    int targetQuantity;

    /**
     * GP corresponding to the 1 percent tax that is levied on the seller for this transaction.
     * This field is not included when purchasing items.
     */
    @Nullable
    Long sellerTax;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(4);
        fields.add(
            new Field("Status", Field.formatBlock("", GrandExchangeNotifier.getHumanStatus(status)))
        );

        if (status == GrandExchangeOfferState.BUYING || status == GrandExchangeOfferState.SELLING) {
            fields.add(
                new Field("Target Quantity", Field.formatBlock("", String.valueOf(targetQuantity)))
            );
        }

        if (sellerTax != null && sellerTax > 0) {
            fields.add(
                new Field("After-Tax Value", ItemUtils.formatGold(item.getTotalPrice() - sellerTax))
            );
        }

        if (marketPrice > 0) {
            fields.add(
                new Field("Market Unit Price", ItemUtils.formatGold(marketPrice))
            );
        }

        return fields;
    }
}
