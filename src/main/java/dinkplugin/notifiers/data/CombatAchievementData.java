package dinkplugin.notifiers.data;

import dinkplugin.domain.CombatAchievementTier;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class CombatAchievementData extends NotificationData {
    CombatAchievementTier tier;
    String task;
    int taskPoints;
}
