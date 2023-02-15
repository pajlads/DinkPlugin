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

    @Override
    public List<Field> getFields() {
        if (petName == null || petName.isEmpty())
            return super.getFields();

        List<Field> fields = new ArrayList<>();
        fields.add(new Field("Name", Field.format("", petName)));
        if (milestone != null)
            fields.add(new Field("Milestone", Field.format("", milestone)));
        return fields;
    }
}
