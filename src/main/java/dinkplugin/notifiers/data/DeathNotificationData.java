package dinkplugin.notifiers.data;

import lombok.Value;

@Value
public class DeathNotificationData {
    Long valueLost;
    boolean isPvp;
    String pker;
}
