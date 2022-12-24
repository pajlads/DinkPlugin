package dinkplugin.notifiers.data;

import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Duration;

@Value
public class BossNotificationData {
    String boss;
    Integer count;
    String gameMessage;
    Duration time;
    @Accessors(fluent = true)
    Boolean isPersonalBest;
}
