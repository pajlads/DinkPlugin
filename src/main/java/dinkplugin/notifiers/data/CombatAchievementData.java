package dinkplugin.notifiers.data;

import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class CombatAchievementData extends NotificationData {

    /**
     * The tier of the task that was just completed.
     */
    @NotNull
    CombatAchievementTier tier;

    /**
     * The name of the task that was just completed.
     */
    @NotNull
    String task;

    /**
     * The number of points rewarded for completing this task.
     */
    int taskPoints;

    /**
     * The total points that have been earned by the player from tasks across tiers.
     */
    int totalPoints;

    /**
     * The marginal points that have been obtained towards the next rewards unlock.
     * <p>
     * Formula: totalPoints - previousUnlockThreshold
     * <p>
     * Range: [0, tierTotalPoints - 1]
     * <p>
     * This field is <i>not</i> populated if the player has completed all tiers.
     */
    @Nullable
    Integer tierProgress;

    /**
     * The total points within a tier to unlock the next reward.
     * <p>
     * Formula: {@code tierProgress + pointsUntilNextUnlock}
     * <p>
     * This can be transformed to the cumulative points threshold via:
     * {@code (totalPoints - tierProgress) + tierTotalPoints}
     * <p>
     * This field is <i>not</i> populated if the player has completed all tiers.
     */
    @Nullable
    Integer tierTotalPoints;

    /**
     * The tier whose rewards were just unlocked,
     * <i>if</i> the player just completed the tier.
     */
    @Nullable
    CombatAchievementTier justCompletedTier;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(3);
        fields.add(new Field("Points Earned", Field.formatBlock(null, String.valueOf(taskPoints))));
        fields.add(new Field("Total Points", Field.formatBlock(null, String.valueOf(totalPoints))));
        if (tierProgress != null && tierTotalPoints != null)
            fields.add(new Field("Next Unlock Progress", Field.formatProgress(tierProgress, tierTotalPoints)));
        return fields;
    }
}
