package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiary;
import lombok.EqualsAndHashCode;
import lombok.Value;

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

}
