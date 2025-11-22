package dinkplugin.notifiers.data;

import dinkplugin.util.Sanitizable;
import dinkplugin.util.SerializedPet;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("world", world);
        if (collectionLog != null) m.put("collectionLog", collectionLog.sanitized());
        if (combatAchievementPoints != null) m.put("combatAchievementPoints", combatAchievementPoints.sanitized());
        if (achievementDiary != null) m.put("achievementDiary", achievementDiary.sanitized());
        if (achievementDiaryTasks != null) m.put("achievementDiaryTasks", achievementDiaryTasks.sanitized());
        if (barbarianAssault != null) m.put("barbarianAssault", barbarianAssault.sanitized());
        if (skills != null) m.put("skills", skills.sanitized());
        if (questCount != null) m.put("questCount", questCount.sanitized());
        if (questPoints != null) m.put("questPoints", questPoints.sanitized());
        if (slayer != null) m.put("slayer", slayer.sanitized());
        if (pets != null) m.put("pets", pets.stream().map(SerializedPet::sanitized).collect(Collectors.toList()));
        return m;
    }

    @Value
    public static class SkillData implements Sanitizable {
        long totalExperience;
        int totalLevel;
        Map<String, Integer> levels;
        Map<String, Integer> experience;

        @Override
        public Map<String, Object> sanitized() {
            return Map.of("totalExperience", totalExperience, "totalLevel", totalLevel, "levels", levels, "experience", experience);
        }
    }

    @Value
    public static class BarbarianAssault implements Sanitizable {
        int highGambleCount;

        @Override
        public Map<String, Object> sanitized() {
            return Map.of("highGambleCount", highGambleCount);
        }
    }

    @Value
    public static class SlayerData implements Sanitizable {
        int points;
        int streak;

        @Override
        public Map<String, Object> sanitized() {
            return Map.of("points", points, "streak", streak);
        }
    }
}
