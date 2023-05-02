package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import dinkplugin.util.ItemUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class GroupStorageNotificationData extends NotificationData {
    Collection<SerializedItemStack> deposits;
    Collection<SerializedItemStack> withdrawals;
    long netValue;

    @Override
    public List<Field> getFields() {
        return Collections.singletonList(
            new Field("Net Value", ItemUtils.formatGold(netValue))
        );
    }
}
