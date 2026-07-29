package dinkplugin.notifiers.data;

import dinkplugin.util.Sanitizable;
import dinkplugin.util.Utils;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

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

    public static AmascutMetadata of(Client client) {
        Collection<String> party = Utils.getAmascutTombsParty(client);
        int rewardPoints = client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSCORE);
        int raidLevel = client.getVarbitValue(VarbitID.TOA_CLIENT_RAID_LEVEL);
        int raidDamage = client.getVarbitValue(VarbitID.TOA_DAMAGE_DONE);
        int teamSize = Math.max(
            Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P0), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P1), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P2), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P3), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P4), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P5), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P6), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P7), 1),
            1);

        // See https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Loot_mechanics
        double petProbability = calcProbability(rewardPoints, 350_000, 700,
            Math.min(raidLevel, 400) + Math.max(Math.min(raidLevel, 550) - 400, 0) / 3.0);
        double weight = 1.0 / teamSize; // approximation for personal points / party points; unlike pets, only one party member can receive a unique
        double purpleProbability = weight * calcProbability(rewardPoints, 10_500, 20,
            Math.min(raidLevel, 310) + Math.max(Math.min(raidLevel, 430) - 310, 0) / 3.0 + Math.max(raidLevel - 430, 0) / 6.0);

        return new AmascutMetadata(teamSize, party, rewardPoints, raidLevel, raidDamage, petProbability, purpleProbability);
    }

    private static double calcProbability(int rewardPoints, int baseDivisor, int levelMultiplier, double scaledRaidLevel) {
        final double maxProbability = 0.55;
        return Math.min(0.01 * rewardPoints / (baseDivisor - levelMultiplier * scaledRaidLevel), maxProbability);
    }

}
