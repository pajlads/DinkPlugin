package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.util.QuantityFormatter;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class TradeNotificationData extends NotificationData {
    String counterparty;
    List<SerializedItemStack> receivedItems;
    List<SerializedItemStack> discardedItems;
    long grossValue;
    long netValue;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(new Field("Gross Value", ItemUtils.formatGold(grossValue)));
        String formattedNet = (netValue >= 0 ? "+ " : "- ") + QuantityFormatter.quantityToStackSize(Math.abs(netValue)) + " gp";
        fields.add(new Field("Net Value", Field.formatBlock("diff", formattedNet)));
        return fields;
    }
}
