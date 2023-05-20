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

import static net.runelite.api.Experience.MAX_REAL_LEVEL;

@Value
@EqualsAndHashCode(callSuper = false)
public class LevelNotificationData extends NotificationData {
    Map<String, Integer> levelledSkills;
    Map<String, Integer> allSkills;
    CombatLevel combatLevel;

    @Override
    public List<Field> getFields() {
        if (levelledSkills.containsValue(MAX_REAL_LEVEL)) {
            Collection<String> maxed = allSkills.entrySet().stream()
                .filter(e -> e.getValue() >= MAX_REAL_LEVEL)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));

            return Collections.singletonList(
                new Field(
                    "Total Skills at Level 99+",
                    Field.formatBlock("", maxed.size() + ": " + String.join(", ", maxed))
                )
            );
        }

        return super.getFields();
    }

    @Value
    public static class CombatLevel {
        int value;
        Boolean increased;
    }
}
