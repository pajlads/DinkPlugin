package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeathNotificationData extends NotificationData {
    Long valueLost;
    boolean isPvp;
    String pker;
    List<SerializedItemStack> keptItems;
    List<SerializedItemStack> lostItems;
}
