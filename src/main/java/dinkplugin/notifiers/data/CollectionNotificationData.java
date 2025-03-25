package dinkplugin.notifiers.data;

import dinkplugin.domain.CollectionLogRank;
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

    /**
     * The current rank after unlocking this collection log entry.
     */
    @Nullable
    CollectionLogRank currentRank;

    /**
     * The number of entries that have been completed since the latest rank.
     */
    @Nullable
    Integer rankProgress;

    /**
     * The number of entries remaining until the next rank unlock.
     */
    @Nullable
    Integer logsNeededForNextRank;

    /**
     * The next rank that can be unlocked.
     */
    @Nullable
    CollectionLogRank nextRank;

    /**
     * The previous rank, if it was just completed (i.e., {@code rankProgress} is zero).
     */
    @Nullable
    CollectionLogRank justCompletedRank;

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
        List<Field> fields = new ArrayList<>(6);
        if (completedEntries != null && totalEntries != null) {
            fields.add(
                new Field("Completed", Field.formatProgress(completedEntries, totalEntries))
            );
        }
        if (currentRank != null) {
            fields.add(
                new Field("Rank", Field.formatBlock("", currentRank.toString()))
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
            fields.add(Field.ofLuck(dropRate, dropperKillCount));
        }
        return fields;
    }
}
