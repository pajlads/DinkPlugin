package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

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

    @Override
    public List<Field> getFields() {
        boolean groupKnown = StringUtils.isNotBlank(groupName);
        List<Field> fields = new ArrayList<>(groupKnown ? 2 : 1);
        if (groupKnown) {
            fields.add(new Field("Group", Field.formatBlock(null, groupName)));
        }
        fields.add(new Field("Net Value", ItemUtils.formatGold(netValue)));
        return fields;
    }
}
