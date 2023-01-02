package dinkplugin.notifiers.data;

import com.google.gson.annotations.JsonAdapter;
import dinkplugin.util.DurationAdapter;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Duration;

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
}
