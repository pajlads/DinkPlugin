package dinkplugin.notifiers.data;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = false)
public class AmascutPurpleNotificationData extends NotificationData {
    Collection<String> party;
    int rewardPoints;
    int raidLevels;
    double probability;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of(
            "party", Objects.requireNonNullElse(party, Collections.emptyList()),
            "rewardPoints", rewardPoints,
            "raidLevels", raidLevels,
            "probability", probability
        );
    }
}
