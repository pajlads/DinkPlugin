package dinkplugin.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {
    private static final int[] FACTORIALS;

    public double binomialProbability(double p, int nTrials, int kSuccess) {
        // https://en.wikipedia.org/wiki/Binomial_distribution#Probability_mass_function
        return binomialCoefficient(nTrials, kSuccess) * Math.pow(p, kSuccess) * Math.pow(1 - p, nTrials - kSuccess);
    }

    private int binomialCoefficient(int n, int k) {
        assert n < FACTORIALS.length && k <= n && k >= 0;
        return FACTORIALS[n] / (FACTORIALS[k] * FACTORIALS[n - k]); // https://en.wikipedia.org/wiki/nCk
    }

    static {
        // precompute factorials from 0 to 9 for n-choose-k formula
        int n = 10; // max rolls in npc_drops.json is 9 (for Bloodthirsty Leagues IV tier 5 relic)
        int[] facts = new int[n];
        facts[0] = 1; // 0! = 1
        for (int i = 1; i < n; i++) {
            facts[i] = i * facts[i - 1];
        }
        FACTORIALS = facts;
    }
}
