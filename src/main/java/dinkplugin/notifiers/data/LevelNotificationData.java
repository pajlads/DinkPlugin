package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.Sanitizable;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static dinkplugin.notifiers.LevelNotifier.LEVEL_FOR_MAX_XP;
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
            return Collections.singletonList(
                new Field(
                    "Total Skills at Level 99+",
                    collectMaxedSkills(MAX_REAL_LEVEL)
                )
            );
        }

        if (levelledSkills.containsValue(LEVEL_FOR_MAX_XP)) {
            return Collections.singletonList(
                new Field(
                    "Total Skills at Max XP",
                    collectMaxedSkills(LEVEL_FOR_MAX_XP)
                )
            );
        }

        return super.getFields();
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("levelledSkills", levelledSkills);
        m.put("allSkills", allSkills);
        if (combatLevel != null) m.put("combatLevel", combatLevel.sanitized());
        return m;
    }

    private String collectMaxedSkills(int minLevel) {
        Collection<String> maxed = allSkills.entrySet().stream()
            .filter(e -> e.getValue() >= minLevel)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(TreeSet::new));
        return Field.formatBlock("", maxed.size() + ": " + String.join(", ", maxed));
    }

    @Value
    public static class CombatLevel implements Sanitizable {
        int value;
        Boolean increased;

        @Override
        public Map<String, Object> sanitized() {
            var m = new HashMap<String, Object>();
            m.put("value", value);
            if (increased != null) m.put("increased", increased);
            return m;
        }
    }
}
