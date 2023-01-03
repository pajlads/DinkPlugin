package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelNotificationData extends NotificationData {
    Map<String, Integer> levelledSkills;
    Map<String, Integer> allSkills;

    @Override
    public List<Field> getFields() {
        if (levelledSkills.values().stream().anyMatch(lvl -> lvl >= 99)) {
            return Collections.singletonList(
                new Field(
                    "Total 99+ Skills",
                    String.format("```\n%d\n```", allSkills.values().stream().filter(lvl -> lvl >= 99).count())
                )
            );
        }

        return super.getFields();
    }
}
