package dinkplugin.notifiers.data;

import dinkplugin.domain.AchievementDiaries;
import lombok.Value;

@Value
public class DiaryNotificationData {
    String area;
    AchievementDiaries.Difficulty difficulty;
    int total;
}
