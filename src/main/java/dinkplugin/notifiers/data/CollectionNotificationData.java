package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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

    @Override
    public List<Field> getFields() {
        if (completedEntries == null || totalEntries == null)
            return super.getFields();

        return Collections.singletonList(
            new Field("Completed Entries", Field.formatProgress(completedEntries, totalEntries))
        );
    }
}
