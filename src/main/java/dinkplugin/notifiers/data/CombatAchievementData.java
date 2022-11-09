package dinkplugin.notifiers.data;

import dinkplugin.domain.CombatAchievementTier;
import lombok.Value;

@Value
public class CombatAchievementData {
    CombatAchievementTier tier;
    String task;
}
