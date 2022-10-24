package dinkplugin.notifiers.data;

import lombok.Value;

import java.util.List;

@Value
public class LootNotificationData {
    List<SerializedItemStack> items;
    String source;
}
