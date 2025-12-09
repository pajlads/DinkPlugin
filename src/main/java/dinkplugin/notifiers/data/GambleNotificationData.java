package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class GambleNotificationData extends NotificationData {
    int gambleCount;
    List<SerializedItemStack> items;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of(
            "gambleCount", gambleCount,
            "items", items.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList())
        );
    }
}
