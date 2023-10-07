package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class SpeedrunNotificationData extends NotificationData {
    /**
     * The name of the quest (e.g. "Cook's Assistant")
     */
    @NonNull
    String questName;

    /**
     * The player's personal best of this quest
     */
    @NonNull
    String personalBest;

    /**
     * The time it took the player to finish the quest
     */
    @NonNull
    String currentTime;

    /**
     * Denotes whether this run was a new personal best or not.
     * If the player ties with their previous personal best, this will be set to false.
     */
    boolean isPersonalBest;
}
