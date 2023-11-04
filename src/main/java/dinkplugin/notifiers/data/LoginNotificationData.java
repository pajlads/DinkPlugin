package dinkplugin.notifiers.data;

import dinkplugin.util.SerializedPet;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class LoginNotificationData extends NotificationData {
    int world;

    @Nullable // requires QUEST_TAB==0; i.e., character summary is selected
    Progress collectionLog;
    Progress combatAchievementPoints;
    Progress achievementDiary;
    Progress achievementDiaryTasks;
    BarbarianAssault barbarianAssault;
    SkillData skills;
    Progress questCount;
    Progress questPoints;
    SlayerData slayer;
    @Nullable // requires Chat Commands plugin to be enabled
    List<SerializedPet> pets;

    @Value
    public static class SkillData {
        long totalExperience;
        int totalLevel;
        Map<String, Integer> levels;
        Map<String, Integer> experience;
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
