package dinkplugin.notifiers.data;

import lombok.Value;

import java.util.List;

@Value
public class ClueNotificationData {
    String clueType;
    int numberCompleted;
    List<SerializedItemStack> items;
}
