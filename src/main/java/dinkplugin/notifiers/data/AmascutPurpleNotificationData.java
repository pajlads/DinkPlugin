package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;

@Value
@EqualsAndHashCode(callSuper = false)
public class AmascutPurpleNotificationData extends NotificationData {
    Collection<String> party;
    int rewardPoints;
    int raidLevels;
    double probability;
}
