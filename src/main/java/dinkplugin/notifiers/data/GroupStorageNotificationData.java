package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        if (groupName != null && !groupName.isBlank())
            fields.add(new Field("Group", Field.formatBlock(null, groupName)));
        if (includePrice) {
            fields.add(new Field("Net Value (GE)", ItemUtils.formatGold(netValue)));
        }
        return fields;
    }
}
