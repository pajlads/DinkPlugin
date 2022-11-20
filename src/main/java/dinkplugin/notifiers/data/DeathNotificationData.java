package dinkplugin.notifiers.data;

import lombok.Value;

import java.util.List;

@Value
public class DeathNotificationData {
    Long valueLost;
    boolean isPvp;
    String pker;
    List<SerializedItemStack> keptItems;
    List<SerializedItemStack> lostItems;
}
