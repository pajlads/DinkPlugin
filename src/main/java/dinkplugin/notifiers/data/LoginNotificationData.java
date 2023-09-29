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
    Progress collectionLog;
    Progress combatAchievementPoints;
    Progress achievementDiary;
    BarbarianAssault barbarianAssault;
    SkillData skills;
    Progress questCount;
    Progress questPoints;
    SlayerData slayer;

    @Value
    public static class SkillData {
        long totalExperience;
        int totalLevel;
        Map<String, Integer> levels;
    }

    @Value
    public static class BarbarianAssault {
        int highGambleCount;
    }

    @Value
    public static class SlayerData {
        int points;
        int streak;
    }
}
