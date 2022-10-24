package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class SpeedrunNotificationData extends QuestNotificationData {
    String personalBest;
    String currentTime;

    public SpeedrunNotificationData(String questName, String personalBest, String currentTime) {
        super(questName);
        this.personalBest = personalBest;
        this.currentTime = currentTime;
    }
}
