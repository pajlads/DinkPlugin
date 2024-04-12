package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Drop;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LootNotificationData extends NotificationData {
    List<SerializedItemStack> items;
    String source;
    LootRecordType category;

    @Nullable
    Integer killCount;

    @Nullable
    Double rarestProbability;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(3);
        if (killCount != null) {
            fields.add(
                new Field(
                    Drop.getAction(category) + " Count",
                    Field.formatBlock("", QuantityFormatter.quantityToStackSize(killCount))
                )
            );
        }
        fields.add(
            new Field(
                "Total Value",
                ItemUtils.formatGold(items.stream().mapToLong(SerializedItemStack::getTotalPrice).sum())
            )
        );
        if (rarestProbability != null) {
            fields.add(new Field("Item Rarity", Field.formatProbability(rarestProbability)));
        }
        return fields;
    }
}
