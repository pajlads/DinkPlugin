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
public class AmascutTracker {

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

    public double getPetProbability() {
        // See https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Tertiary_rewards
        return calcProbability(getPersonalContribution(), 350_000, 700,
            Math.min(raidLevel, 400) + Math.max(Math.min(raidLevel, 550) - 400, 0) / 3.0);
    }

    public double getPurpleProbability() {
        // See https://oldschool.runescape.wiki/w/Chest_(Tombs_of_Amascut)#Uniques
        double weight = 1.0 * getPersonalContribution() / partyScore; // unlike pets, only one party member can receive a unique
        return weight * calcProbability(partyScore, 10_500, 20,
            Math.min(raidLevel, 310) + Math.max(Math.min(raidLevel, 430) - 310, 0) / 3.0 + Math.max(raidLevel - 430, 0) / 6.0);
    }

    private static double calcProbability(int rewardPoints, int baseDivisor, int levelMultiplier, double scaledRaidLevel) {
        final double maxProbability = 0.55;
        return Math.min(0.01 * rewardPoints / (baseDivisor - levelMultiplier * scaledRaidLevel), maxProbability);
    }

}
