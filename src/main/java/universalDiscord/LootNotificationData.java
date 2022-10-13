package universalDiscord;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LootNotificationData {
    private List<SerializedItemStack> items = new ArrayList<>();
    private String source;
}
