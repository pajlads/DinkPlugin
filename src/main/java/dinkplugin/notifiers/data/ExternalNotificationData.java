package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExternalNotificationData extends NotificationData {
    String sourcePlugin;
    @EqualsAndHashCode.Include
    transient List<Field> fields;
    Map<String, Object> metadata;
}
