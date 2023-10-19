package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class DiaryNotificationData extends NotificationData {

    /**
     * The area of the diary that was just completed.
     */
    String area;

    /**
     * The difficulty of the diary that was just completed.
     */
    AchievementDiary.Difficulty difficulty;

    /**
     * The number of diaries that have been completed across all regions and difficulties.
     */
    int total;

    /**
     * The number of diary tasks that have been completed across all regions and difficulties.
     */
    int tasksCompleted;

    /**
     * The total number of diary tasks within the game across all regions and difficulties.
     */
    int tasksTotal;

    /**
     * The number of diary tasks completed within this specific area.
     */
    int areaTasksCompleted;

    /**
     * The total number of diary tasks possible within this specific area.
     */
    int areaTasksTotal;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);

        if (areaTasksCompleted > 0 && areaTasksTotal > 0) {
            fields.add(
                new Field(area + " Progress", Field.formatProgress(areaTasksCompleted, areaTasksTotal))
            );
        }

        if (tasksCompleted > 0 && tasksTotal > 0) {
            fields.add(
                new Field("Overall Progress", Field.formatProgress(tasksCompleted, tasksTotal))
            );
        }

        return fields;
    }
}
