package dinkplugin;

import lombok.Data;

import java.util.List;

@Data
public class ClueNotificationData {
    private String clueType;
    private int numberCompleted;
private List<SerializedItemStack> items;
}
