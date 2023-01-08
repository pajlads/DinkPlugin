package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiary;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class DiaryNotificationData extends NotificationData {
    String area;
    AchievementDiary.Difficulty difficulty;
    int total;
}
