package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class ExternalNotificationData extends NotificationData {
    String sourcePlugin;
    @EqualsAndHashCode.Include
    transient List<Field> fields;
    Map<String, Object> metadata;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of(
            "sourcePlugin", sourcePlugin,
            "metadata", Objects.requireNonNullElse(metadata, Collections.emptyMap())
        );
    }
}
