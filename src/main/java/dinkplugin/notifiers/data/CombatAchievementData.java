package dinkplugin.notifiers.data;

import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class CombatAchievementData extends NotificationData {
    CombatAchievementTier tier;
    String task;
    int taskPoints;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(1);
        fields.add(
            new Field("Points Earned", Field.formatBlock(null, String.valueOf(taskPoints)))
        );
        return fields;
    }
}
