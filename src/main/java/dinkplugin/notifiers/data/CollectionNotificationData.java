package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Drop;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.util.QuantityFormatter;
import net.runelite.http.api.loottracker.LootRecordType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class CollectionNotificationData extends NotificationData {

    @NotNull
    String itemName;

    @Nullable
    Integer itemId;

    @Nullable
    Long price;

    @Nullable
    Integer completedEntries;

    @Nullable
    Integer totalEntries;

    @Nullable
    String dropperName;

    @Nullable
    LootRecordType dropperType;

    @Nullable
    Integer dropperKillCount;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        if (completedEntries != null && totalEntries != null) {
            fields.add(
                new Field("Completed Entries", Field.formatProgress(completedEntries, totalEntries))
            );
        }
        if (dropperKillCount != null) {
            assert dropperName != null && dropperType != null;
            fields.add(
                new Field(
                    Drop.getAction(dropperType) + " Count: " + dropperName,
                    Field.formatBlock("", QuantityFormatter.quantityToStackSize(dropperKillCount))
                )
            );
        }
        return fields;
    }
}
