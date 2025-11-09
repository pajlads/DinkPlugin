package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class GroupStorageNotificationData extends NotificationData {
    Collection<SerializedItemStack> deposits;
    Collection<SerializedItemStack> withdrawals;
    long netValue;
    String groupName;
    transient boolean includePrice;

    @Override
    public List<Field> getFields() {
        List<Field> fields = new ArrayList<>(2);
        if (StringUtils.isNotBlank(groupName))
            fields.add(new Field("Group", Field.formatBlock(null, groupName)));
        if (includePrice) {
            fields.add(new Field("Net Value (GE)", ItemUtils.formatGold(netValue)));
        }
        return fields;
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        m.put("deposits", deposits.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        m.put("withdrawals", withdrawals.stream().map(SerializedItemStack::sanitized).collect(Collectors.toList()));
        m.put("netValue", netValue);
        if (groupName != null) m.put("groupName", groupName);
        return m;
    }
}
