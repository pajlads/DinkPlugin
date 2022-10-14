package dinkplugin;

import lombok.Data;

import java.time.Duration;

@Data
public class SpeedrunPBNotificationData extends QuestNotificationData {
    private Duration personalBest;
}
