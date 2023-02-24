package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpeedrunNotificationData extends NotificationData {
    String questName;
    String personalBest;
    String currentTime;
}
