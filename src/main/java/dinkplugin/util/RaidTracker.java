package dinkplugin.util;

import lombok.Getter;
import lombok.NoArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;

@Singleton
@NoArgsConstructor
public class RaidTracker {

    private static final int[] PARTY_VARBS = {
        VarbitID.TOA_CLIENT_P0, VarbitID.TOA_CLIENT_P1, VarbitID.TOA_CLIENT_P2, VarbitID.TOA_CLIENT_P3,
        VarbitID.TOA_CLIENT_P4, VarbitID.TOA_CLIENT_P5, VarbitID.TOA_CLIENT_P6, VarbitID.TOA_CLIENT_P7
    };

    @Inject
    private Client client;

    @Getter
    private int raidLevel;

    @Getter
    private int partyScore;

    private int personalContribution;

    @Getter
    private int damageDone;

    @Getter
    private int teamSize;

    @Getter
    private Collection<String> partyMembers = Collections.emptyList();

    private boolean checkPartyMembers;

    public void onVarbit(VarbitChanged e) {
        if (e.getValue() <= 0) {
            return;
        }

        if (e.getVarbitId() == VarbitID.TOA_CLIENT_RAID_LEVEL) {
            this.raidLevel = e.getValue();
        } else if (e.getVarbitId() == VarbitID.RAIDS_CLIENT_PARTYSCORE) {
            this.partyScore = e.getValue();
        } else if (e.getVarpId() == VarPlayerID.TOA_PERSONAL_CONTRIBUTION) {
            this.personalContribution = e.getValue();
        } else if (e.getVarbitId() == VarbitID.TOA_DAMAGE_DONE) {
            this.damageDone = e.getValue();
        } else if (e.getVarbitId() >= PARTY_VARBS[0] && e.getVarbitId() <= PARTY_VARBS[PARTY_VARBS.length - 1]) {
            this.checkPartyMembers = true;
        } else if (e.getVarbitId() == VarbitID.RAIDS_CLIENT_PARTYSIZE) {
            this.teamSize = e.getValue();
        } else if (e.getVarpId() == VarPlayerID.RAIDS_PLAYERSCORE) {
            this.personalContribution = e.getValue();
        }
    }

    public void onTick() {
        if (this.checkPartyMembers) {
            this.checkPartyMembers = false;

            int size = Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P0), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P1), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P2), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P3), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P4), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P5), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P6), 1) +
                Math.min(client.getVarbitValue(VarbitID.TOA_CLIENT_P7), 1);

            if (size >= 1) {
                this.teamSize = size;
                this.partyMembers = Utils.getAmascutTombsParty(client);
            }
        }
    }

    public int getPersonalContribution() {
        return personalContribution > 0
            ? personalContribution
            : partyScore / Math.max(teamSize, 1);
    }

    public double getXericPetProbability() {
        // https://oldschool.runescape.wiki/w/Ancient_chest#Unique_drop_table
        final double uniqueDropPetRate = 1.0 / 53; // likelihood of a pet drop from a successful unique roll
        final int maxPointsPerRoll = 570_000; // "chance is capped at 65.7% (570,000 points) - any further points will be sent to roll for a second unique loot"
        final double pointsPerPct = 867_600; // "For every 8,676 total points obtained, a 1% chance to obtain a unique loot is given"
        final double rarityForMaxRoll = (maxPointsPerRoll / pointsPerPct) * uniqueDropPetRate; // 1.2% is pet chance for unique roll with max points

        int totalPoints = partyScore <= 0 ? 26_025 : partyScore;
        int numMaxRolls = Math.min(totalPoints / maxPointsPerRoll, 6); // "Up to six unique rewards can be obtained per raid"
        int lastRollPoints = totalPoints % maxPointsPerRoll; // remaining points for a non-max unique roll
        double lastRollProb = (lastRollPoints / pointsPerPct) * uniqueDropPetRate; // Prob(unique) * P(pet | unique) = P(pet)

        // Similar to cumulative geometric: 1 - Prob(all rolls failed to produce a unique) = Prob(at least one unique)
        double partyProbability = 1 - Math.pow(1 - rarityForMaxRoll, numMaxRolls) * (1 - lastRollProb);

        // Party adjustment: pet is more likely to be allocated to players with greater points
        double weight = 1.0 * getPersonalContribution() / totalPoints;

        // Prob(party rolls a pet) * P(local player gets pet | party rolls a pet) = P(local player gets pet)
        return partyProbability * weight;
    }

    public double getAmascutPetProbability() {
        // See https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Tertiary_rewards
        return calcProbability(getPersonalContribution(), 350_000, 700,
            Math.min(raidLevel, 400) + Math.max(Math.min(raidLevel, 550) - 400, 0) / 3.0);
    }

    public double getAmascutPurpleProbability() {
        // See https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Uniques
        int personalContribution = getPersonalContribution();
        double weight = personalContribution > 0 && partyScore > 0
            ? 1.0 * personalContribution / partyScore : 1.0; // unlike pets, only one party member can receive a unique
        return weight * calcProbability(partyScore, 10_500, 20,
            Math.min(raidLevel, 310) + Math.max(Math.min(raidLevel, 430) - 310, 0) / 3.0 + Math.max(raidLevel - 430, 0) / 6.0);
    }

    private static double calcProbability(int rewardPoints, int baseDivisor, int levelMultiplier, double scaledRaidLevel) {
        final double maxProbability = 0.55;
        return Math.min(0.01 * rewardPoints / (baseDivisor - levelMultiplier * scaledRaidLevel), maxProbability);
    }

}
