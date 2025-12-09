package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Drop;
import dinkplugin.util.ItemUtils;
import lombok.Value;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
public class LootNotificationData extends NotificationData {
    Collection<SerializedItemStack> items;
    String source;
    LootRecordType category;

    @Nullable
    Integer killCount;

    @Nullable
    Double rarestProbability;

    @Nullable
    Collection<String> party;

    @Nullable
    Integer npcId;

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

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("items", items.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        m.put("source", source);
        m.put("category", category);
        if (killCount != null) m.put("killCount", killCount);
        if (rarestProbability != null) m.put("rarestProbability", rarestProbability);
        if (party != null) m.put("party", party);
        if (npcId != null) m.put("npcId", npcId);
        return m;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LootNotificationData)) return false;
        LootNotificationData that = (LootNotificationData) o;
        return category == that.category && Objects.equals(source, that.source)
            && Objects.equals(killCount, that.killCount) && Objects.equals(rarestProbability, that.rarestProbability)
            && items.containsAll(that.items) && that.items.containsAll(items); // for ease of testing
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, source, category, killCount, rarestProbability);
    }
}
