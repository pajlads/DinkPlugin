package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeagueRelicTier {
    UNKNOWN(-1),
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
    private final int defaultPoints;

}
