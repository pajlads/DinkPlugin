package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiary;
import lombok.Value;

@Value
public class DiaryNotificationData {
    String area;
    AchievementDiary.Difficulty difficulty;
    int total;
}
