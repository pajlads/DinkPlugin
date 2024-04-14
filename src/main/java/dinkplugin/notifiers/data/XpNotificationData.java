package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.api.Experience;
import net.runelite.api.Skill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Value
@EqualsAndHashCode(callSuper = false)
public class XpNotificationData extends NotificationData {
    Map<Skill, Integer> xpData;
    Collection<Skill> milestoneAchieved;
    int interval;
    transient String totalXp;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(new Field("Total XP", Field.formatBlock("", totalXp)));

        SortedSet<String> maxed = new TreeSet<>();
        for (var entry : xpData.entrySet()) {
            if (entry.getValue() >= Experience.MAX_SKILL_XP) {
                maxed.add(entry.getKey().getName());
            }
        }
        if (!maxed.isEmpty()) {
            fields.add(new Field("Maxed Skills", Field.formatBlock("", String.join(", ", maxed))));
        }

        return fields;
    }
}
