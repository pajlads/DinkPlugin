package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.util.QuantityFormatter;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ClueNotificationData extends NotificationData {
    String clueType;
    int numberCompleted;
    List<SerializedItemStack> items;

    @Override
    public List<Field> getFields() {
        return Collections.singletonList(
            new Field(
                "Total Value",
                QuantityFormatter.quantityToStackSize(items.stream().mapToLong(SerializedItemStack::getTotalPrice).sum()) + " gp"
            )
        );
    }
}
