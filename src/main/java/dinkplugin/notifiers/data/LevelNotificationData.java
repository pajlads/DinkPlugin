package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelNotificationData extends NotificationData {
    Map<String, Integer> levelledSkills;
    Map<String, Integer> allSkills;

    @Override
    public List<Field> getFields() {
        if (levelledSkills.containsValue(99)) {
            Collection<String> maxed = allSkills.entrySet().stream()
                .filter(e -> e.getValue() >= 99)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));

            return Collections.singletonList(
                new Field(
                    "Total Skills at Level 99+",
                    Field.format("", maxed.size() + ": " + String.join(", ", maxed))
                )
            );
        }

        return super.getFields();
    }
}
