package dinkplugin;

import lombok.Data;

@Data
public class SpeedrunNotificationData extends QuestNotificationData {
    private String personalBest;
    private String currentTime;
}
