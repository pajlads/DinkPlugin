package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class QuestNotificationData extends NotificationData {

    @NotNull
    String questName;

    @Nullable
    Integer completedQuests;

    @Nullable
    Integer totalQuests;

    @Override
    public List<Field> getFields() {
        if (completedQuests == null || totalQuests == null)
            return super.getFields();

        return Collections.singletonList(
            new Field("Completed Quests", Field.formatProgress(completedQuests, totalQuests))
        );
    }
}
