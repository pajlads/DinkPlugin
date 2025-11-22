package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("area", area);
        m.put("index", index);
        m.put("tasksCompleted", tasksCompleted);
        if (tasksUntilNextArea != null) m.put("tasksUntilNextArea", tasksUntilNextArea);
        return m;
    }
}
