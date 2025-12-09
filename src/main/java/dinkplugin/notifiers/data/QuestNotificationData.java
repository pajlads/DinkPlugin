package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("questName", questName);
        if (completedQuests != null) m.put("completedQuests", completedQuests);
        if (totalQuests != null) m.put("totalQuests", totalQuests);
        if (questPoints != null) m.put("questPoints", questPoints);
        if (totalQuestPoints != null) m.put("totalQuestPoints", totalQuestPoints);
        return m;
    }
}
