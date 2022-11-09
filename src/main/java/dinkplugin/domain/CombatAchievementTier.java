package dinkplugin.domain;

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

    private final String displayName = this.name().charAt(0) + this.name().substring(1).toLowerCase();

    @Override
    public String toString() {
        return this.displayName;
    }

    public static final Map<String, CombatAchievementTier> TIER_BY_LOWER_NAME = Collections.unmodifiableMap(
        Arrays.stream(values()).collect(Collectors.toMap(t -> t.name().toLowerCase(), Function.identity()))
    );
}
