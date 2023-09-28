package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class LoginNotificationData extends NotificationData {
    int world;

    int collectionCompleted;
    int collectionTotal;

    int combatAchievementPoints;
    int combatAchievementPointsTotal;

    int diaryCompleted;
    int diaryTotal;

    int gambleCount;

    long experienceTotal;
    int levelTotal;
    Map<String, Integer> skillLevels;

    int questsCompleted;
    int questsTotal;

    int questPoints;
    int questPointsTotal;

    int slayerPoints;
    int slayerStreak;
}
