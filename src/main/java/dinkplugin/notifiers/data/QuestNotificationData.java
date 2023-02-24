package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    @Nullable
    Integer questPoints;

    @Nullable
    Integer totalQuestPoints;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);

        if (completedQuests != null && totalQuests != null)
            fields.add(new Field("Completed Quests", Field.formatProgress(completedQuests, totalQuests)));

        if (questPoints != null && totalQuestPoints != null)
            fields.add(new Field("Quest Points", Field.formatProgress(questPoints, totalQuestPoints)));

        return fields;
    }
}
