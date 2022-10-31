package dinkplugin.notifiers.data;

import lombok.Value;

@Value
public class DeathNotificationData {
    Integer valueLost;
    boolean isPvp;
    String pker;
}
