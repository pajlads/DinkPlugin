package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class PetNotificationData extends NotificationData {
    String petName;
    String milestone;
    boolean duplicate;

    @Override
    public List<Field> getFields() {
        if (petName == null || petName.isEmpty())
            return super.getFields();

        List<Field> fields = new ArrayList<>(3);
        fields.add(new Field("Name", Field.formatBlock("", petName)));
        if (duplicate)
            fields.add(new Field("Status", Field.formatBlock("", "Already owned")));
        if (milestone != null)
            fields.add(new Field("Milestone", Field.formatBlock("", milestone)));
        return fields;
    }
}
