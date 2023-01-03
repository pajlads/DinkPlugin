package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

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
                ItemUtils.formatGold(items.stream().mapToLong(SerializedItemStack::getTotalPrice).sum())
            )
        );
    }
}
