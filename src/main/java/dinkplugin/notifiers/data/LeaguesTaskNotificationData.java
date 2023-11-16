package dinkplugin.notifiers.data;

import dinkplugin.domain.LeagueTaskDifficulty;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class LeaguesTaskNotificationData extends NotificationData {

    @NotNull
    String taskName;

    @NotNull
    LeagueTaskDifficulty difficulty;

    int taskPoints;
    int totalPoints;
    int tasksCompleted;

    @Nullable // when player has already unlocked all three regions
    Integer tasksUntilNextArea;

    @Nullable // when player has already unlocked a tier 8 relic (highest)
    Integer pointsUntilNextRelic;

    @Nullable // when player has already earned the dragon trophy (highest)
    Integer pointsUntilNextTrophy;

    @Nullable // when player hasn't even earned the bronze trophy (lowest)
    String earnedTrophy;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(3);
        fields.add(new Field("Total Tasks", Field.formatBlock("", String.valueOf(tasksCompleted))));
        fields.add(new Field("Total Points", Field.formatBlock("", String.valueOf(totalPoints))));
        if (earnedTrophy == null && pointsUntilNextRelic != null) {
            fields.add(new Field("Points until next Relic", Field.formatBlock("", String.valueOf(pointsUntilNextRelic))));
        } else if (pointsUntilNextTrophy != null) {
            fields.add(new Field("Points until next Trophy", Field.formatBlock("", String.valueOf(pointsUntilNextTrophy))));
        }
        return fields;
    }
}
