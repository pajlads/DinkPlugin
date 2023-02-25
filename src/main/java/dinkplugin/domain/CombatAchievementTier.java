package dinkplugin.domain;

import lombok.Getter;
import net.runelite.api.annotations.Varbit;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CombatAchievementTier {
    EASY,
    MEDIUM,
    HARD,
    ELITE,
    MASTER,
    GRANDMASTER;

    /**
     * Varbit ID corresponding to the number of completed tasks within a given tier.
     *
     * @see <a href="https://github.com/Joshua-F/cs2-scripts/blob/master/scripts/%5Bproc,ca_tasks_completed_tier%5D.cs2">Clientscript Reference</a>
     */
    @Getter(onMethod_ = { @Varbit })
    private final int completedVarbitId = 12885 + this.ordinal();
    private final String displayName = this.name().charAt(0) + this.name().substring(1).toLowerCase();

    @Override
    public String toString() {
        return this.displayName;
    }

    public static final Map<String, CombatAchievementTier> TIER_BY_LOWER_NAME = Collections.unmodifiableMap(
        Arrays.stream(values()).collect(Collectors.toMap(t -> t.name().toLowerCase(), Function.identity()))
    );
}
