package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class SlayerNotificationData extends NotificationData {
    String slayerTask;
    String slayerCompleted;
    String slayerPoints;
}
