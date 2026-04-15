package dinkplugin.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LeagueRelicTier {
    UNKNOWN(-1),
    ONE(0),
    TWO(600),
    THREE(1_200),
    FOUR(2_600),
    FIVE(5_200),
    SIX(8_500),
    SEVEN(16_500),
    EIGHT(28_000);

    /**
     * Points required to unlock a relic of a given tier.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Demonic_Pacts_League/Relics">Wiki Reference</a>
     */
    private final int defaultPoints;

}
