package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class LeaguesMasteryNotificationData extends NotificationData {

    /**
     * Melee, Ranged, or Magic.
     */
    String masteryType;

    /**
     * Ranges from 1 to 6.
     */
    int masteryTier;

}
