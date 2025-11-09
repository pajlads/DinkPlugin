package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Map;

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

    @Override
    public Map<String, Object> sanitized() {
        return Map.of("masteryType", masteryType, "masterTier", masteryTier);
    }
}
