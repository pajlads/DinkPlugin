package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class TradeNotificationData extends NotificationData {
    String counterparty;
    List<SerializedItemStack> receivedItems;
    List<SerializedItemStack> givenItems;
    long receivedValue;
    long givenValue;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(new Field("Received Value", ItemUtils.formatGold(receivedValue)));
        fields.add(new Field("Given Value", ItemUtils.formatGold(givenValue)));
        return fields;
    }
}
