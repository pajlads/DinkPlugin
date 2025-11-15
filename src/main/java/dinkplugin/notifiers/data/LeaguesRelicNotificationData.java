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
public class LeaguesRelicNotificationData extends NotificationData {

    @NotNull
    String relic;
    int tier;
    int requiredPoints;
    int totalPoints;

    @Nullable // if relics for all 8 tiers have now been unlocked
    Integer pointsUntilNextTier;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(
            new Field(
                "Total Points",
                Field.formatBlock("", String.valueOf(totalPoints))
            )
        );
        if (pointsUntilNextTier != null) {
            fields.add(
                new Field(
                    "Points until next Relic",
                    Field.formatBlock("", String.valueOf(pointsUntilNextTier))
                )
            );
        }
        return fields;
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("relic", relic);
        m.put("tier", tier);
        m.put("requiredPoints", requiredPoints);
        m.put("totalPoints", totalPoints);
        if (pointsUntilNextTier != null) m.put("pointsUntilNextTier", pointsUntilNextTier);
        return m;
    }
}
