package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum LeagueTaskDifficulty {
    EASY(10),
    MEDIUM(40),
    HARD(80),
    ELITE(200),
    MASTER(400);

    /**
     * Points earned from completed a task of the given difficulty.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Tasks">Wiki Reference</a>
     */
    private final int points;
    private final String displayName = this.name().charAt(0) + this.name().substring(1).toLowerCase();

    @Override
    public String toString() {
        return this.displayName;
    }

    public static final Map<String, LeagueTaskDifficulty> TIER_BY_LOWER_NAME = Collections.unmodifiableMap(
        Arrays.stream(values()).collect(Collectors.toMap(t -> t.name().toLowerCase(), Function.identity()))
    );
}
