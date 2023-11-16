package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LeaguesRelicNotificationData extends NotificationData {
    String relic;
    Integer tier;
    int totalPointsEarned;
    Integer pointsUntilNextTier;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        fields.add(
            new Field(
                "Points Earned",
                Field.formatBlock("", String.valueOf(totalPointsEarned))
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
}
