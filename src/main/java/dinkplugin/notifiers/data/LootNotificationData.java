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
import java.util.Collection;
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

    @Nullable
    Collection<String> party;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(4);
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
        if (party != null && !party.isEmpty()) {
            fields.add(new Field("Party Size", Field.formatBlock("", String.valueOf(party.size()))));
        }
        return fields;
    }
}
