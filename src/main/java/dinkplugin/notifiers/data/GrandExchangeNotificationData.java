package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.notifiers.GrandExchangeNotifier;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.GrandExchangeOfferState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class GrandExchangeNotificationData extends NotificationData {
    int slot;
    GrandExchangeOfferState status;
    SerializedItemStack item;
    long marketPrice;
    int targetQuantity;
    @Nullable Long sellerTax;

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
