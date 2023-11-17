package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LeaguesAreaNotificationData extends NotificationData {

    @NotNull
    String area;

    int index;

    int tasksCompleted;

    @Nullable // if player has already unlocked all three regions
    Integer tasksUntilNextArea;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(
            new Field("Tasks Completed", Field.formatBlock("", String.valueOf(tasksCompleted)))
        );
        if (tasksUntilNextArea != null) {
            fields.add(
                new Field("Tasks until next Area", Field.formatBlock("", String.valueOf(tasksUntilNextArea)))
            );
        }
        return fields;
    }
}
