package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class LoginNotificationData extends NotificationData {
    int world;

    @Nullable // requires QUEST_TAB==0; i.e., character summary is selected
    Integer collectionCompleted;
    @Nullable // requires QUEST_TAB==0; i.e., character summary is selected
    Integer collectionTotal;

    Integer combatAchievementPoints;
    Integer combatAchievementPointsTotal;

    int diaryCompleted;
    int diaryTotal;

    int gambleCount;

    long experienceTotal;
    int levelTotal;
    Map<String, Integer> skillLevels;

    Integer questsCompleted;
    Integer questsTotal;

    Integer questPoints;
    Integer questPointsTotal;

    int slayerPoints;
    int slayerStreak;
}
