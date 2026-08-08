package dinkplugin.notifiers.data;

import dinkplugin.util.RaidTracker;
import dinkplugin.util.Sanitizable;
import lombok.Value;

import java.util.Collection;
import java.util.Map;

@Value
public class AmascutMetadata implements Sanitizable {

    int teamSize;
    transient Collection<String> party;
    int rewardPoints;
    int raidLevel;
    int raidDamage;
    double petProbability;
    double purpleProbability;

    @Override
    public Map<String, Object> sanitized() {
        return Map.of(
            "teamSize", teamSize,
            "rewardPoints", rewardPoints,
            "raidLevel", raidLevel,
            "raidDamage", raidDamage,
            "petProbability", petProbability,
            "purpleProbability", purpleProbability
        );
    }

    public static AmascutMetadata of(RaidTracker tracker) {
        return new AmascutMetadata(tracker.getTeamSize(), tracker.getPartyMembers(), tracker.getPartyScore(),
            tracker.getRaidLevel(), tracker.getDamageDone(), tracker.getAmascutPetProbability(), tracker.getAmascutPurpleProbability());
    }

}
