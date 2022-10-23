package dinkplugin.notifiers.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SpeedrunNotificationData extends QuestNotificationData {
    private String personalBest;
    private String currentTime;
}
