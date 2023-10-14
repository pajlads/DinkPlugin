package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiary;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
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
     * The number of diaries that have been completed (across all regions and difficulties).
     */
    int total;

    /**
     * The number of diary tasks that have been completed (across all regions and difficulties).
     */
    int tasksCompleted;

    /**
     * The total number of diary tasks within the game (across all regions and difficulties).
     */
    int tasksTotal;

    @Override
    public List<Field> getFields() {
        if (tasksCompleted > 0 && tasksTotal > 0) {
            return Collections.singletonList(
                new Field("Overall Task Progress", Field.formatProgress(tasksCompleted, tasksTotal))
            );
        }

        return super.getFields();
    }
}
