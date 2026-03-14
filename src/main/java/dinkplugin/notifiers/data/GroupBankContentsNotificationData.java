package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class GroupBankContentsNotificationData extends NotificationData {
    List<SerializedItemStack> items;
    int slots;

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("slots", slots);
        m.put("items", items.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        return m;
    }
}
