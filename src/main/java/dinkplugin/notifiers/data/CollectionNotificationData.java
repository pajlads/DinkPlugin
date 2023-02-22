package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class CollectionNotificationData extends NotificationData {
    // DecimalFormat is not thread-safe, so ThreadLocal is used.
    // Currently, this is not necessary, but is simple future-proofing in case of a DiscordMessageHandler refactor.
    private static final ThreadLocal<DecimalFormat> PERCENT_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("#.#%"));

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

    @Override
    public List<Field> getFields() {
        if (completedEntries != null && totalEntries != null) {
            String percent = PERCENT_FORMAT.get().format(1.0 * completedEntries / totalEntries);
            return Collections.singletonList(
                new Field(
                    "Completed Entries",
                    Field.formatBlock("", String.format("%d/%d (%s)", completedEntries, totalEntries, percent)))
            );
        }

        return super.getFields();
    }
}
