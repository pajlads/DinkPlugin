package dinkplugin.notifiers.data;

import dinkplugin.domain.CombatAchievementTier;
import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class CombatAchievementData extends NotificationData {

    @NotNull
    CombatAchievementTier tier;

    @NotNull
    String task;

    @Nullable
    Integer tierTasksCompleted;

    @Nullable
    Integer totalTierTasks;

    @Override
    public List<Field> getFields() {
        if (tierTasksCompleted == null || totalTierTasks == null)
            return super.getFields();

        return Collections.singletonList(
            new Field("Tier Progress", Field.formatProgress(tierTasksCompleted, totalTierTasks))
        );
    }
}
