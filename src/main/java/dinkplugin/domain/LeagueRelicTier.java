package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

@Getter
@RequiredArgsConstructor
public enum LeagueRelicTier {
    ONE(0),
    TWO(500),
    THREE(1_200),
    FOUR(2_000),
    FIVE(4_000),
    SIX(7_500),
    SEVEN(15_000),
    EIGHT(24_000);

    /**
     * Points required to unlock a relic of a given tier.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Relics">Wiki Reference</a>
     */
    private final int points;

    public static final NavigableMap<Integer, LeagueRelicTier> TIER_BY_POINTS;

    static {
        NavigableMap<Integer, LeagueRelicTier> tiers = new TreeMap<>();
        for (LeagueRelicTier tier : values()) {
            tiers.put(tier.getPoints(), tier);
        }
        TIER_BY_POINTS = Collections.unmodifiableNavigableMap(tiers);
    }
}
