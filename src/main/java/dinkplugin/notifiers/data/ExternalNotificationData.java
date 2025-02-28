package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExternalNotificationData extends NotificationData {
    String sourcePlugin;
    List<Field> fields;
}
