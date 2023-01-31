package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class GambleNotificationData extends NotificationData {
    int gambleCount;
    List<SerializedItemStack> items;
}
