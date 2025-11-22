package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public Map<String, Object> sanitized() {
        return Map.of(
            "clueType", clueType,
            "numberCompleted", numberCompleted,
            "items", items.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList())
        );
    }
}
