package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeagueRelicTier {
    UNKNOWN(-1),
    ONE(0),
    TWO(750),
    THREE(1_500),
    FOUR(2_500),
    FIVE(5_000),
    SIX(8_000),
    SEVEN(16_000),
    EIGHT(25_000);

    /**
     * Points required to unlock a relic of a given tier.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Relics">Wiki Reference</a>
     */
    private final int defaultPoints;

}
