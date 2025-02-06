package dinkplugin.notifiers.data;

import com.google.gson.annotations.JsonAdapter;
import dinkplugin.message.Field;
import dinkplugin.util.DurationAdapter;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@With
@Value
@EqualsAndHashCode(callSuper = false)
public class BossNotificationData extends NotificationData {
    String boss;
    Integer count;
    String gameMessage;
    @JsonAdapter(DurationAdapter.class)
    Duration time;
    @Accessors(fluent = true)
    Boolean isPersonalBest;
    @Nullable
    @JsonAdapter(DurationAdapter.class)
    Duration personalBest;
    @Nullable
    Collection<String> party;

    @Override
    public List<Field> getFields() {
        if (party != null && !party.isEmpty()) {
            return List.of(new Field("Party Size", Field.formatBlock("", String.valueOf(party.size()))));
        }
        return super.getFields();
    }
}
