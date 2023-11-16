package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LeaguesAreaNotificationData extends NotificationData {
    String area;
    Integer index;
    int tasksCompleted;
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
