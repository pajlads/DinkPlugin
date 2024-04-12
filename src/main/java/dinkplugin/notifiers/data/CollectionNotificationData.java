package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Drop;
import dinkplugin.util.MathUtils;
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

    @Nullable
    Double dropRate;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(5);
        if (completedEntries != null && totalEntries != null) {
            fields.add(
                new Field("Completed", Field.formatProgress(completedEntries, totalEntries))
            );
        }
        if (dropperKillCount != null) {
            assert dropperName != null && dropperType != null;
            fields.add(
                new Field("Source", Field.formatBlock("", dropperName))
            );
            fields.add(
                new Field(
                    Drop.getAction(dropperType) + " Count",
                    Field.formatBlock("", QuantityFormatter.quantityToStackSize(dropperKillCount))
                )
            );
        }
        if (dropRate != null) {
            fields.add(new Field("Drop Rate", Field.formatProbability(dropRate)));
        }
        if (dropperKillCount != null && dropRate != null) {
            double geomCdf = MathUtils.cumulativeGeometric(dropRate, dropperKillCount);
            String percentile = geomCdf < 0.5
                ? "Top " + MathUtils.formatPercentage(geomCdf, 2) + " (Lucky)"
                : "Bottom " + MathUtils.formatPercentage(1 - geomCdf, 2) + " (Unlucky)";
            fields.add(new Field("Luck", Field.formatBlock("", percentile)));
        }
        return fields;
    }
}
