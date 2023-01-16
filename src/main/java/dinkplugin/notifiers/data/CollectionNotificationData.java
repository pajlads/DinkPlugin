package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class CollectionNotificationData extends NotificationData {
    String itemName;
    Integer itemId;
}
