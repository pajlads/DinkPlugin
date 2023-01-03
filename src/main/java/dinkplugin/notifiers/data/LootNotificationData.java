package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LootNotificationData extends NotificationData {
    List<SerializedItemStack> items;
    String source;

    @Override
    public List<Field> getFields() {
        return Collections.singletonList(
            new Field(
                "Total Value",
                ItemUtils.formatGold(items.stream().mapToLong(SerializedItemStack::getTotalPrice).sum())
            )
        );
    }
}
