package dinkplugin.notifiers.data;

import lombok.Value;

@Value
public class BossNotificationData {
    String boss;
    Integer count;
    String gameMessage;
}
