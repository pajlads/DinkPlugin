package dinkplugin.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CollectionLogRanks {
    UNKNOWN(-1),
    BRONZE(100),
    IRON(300),
    STEEL(500),
    BLACK(700),
    MITHRIL(900),
    ADAMANT(1_000),
    RUNE(1_100),
    DRAGON(1_200);
//    GILDED(1_200);

    /**
     * Points required to unlock a relic of a given tier.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Trailblazer_Reloaded_League/Relics">Wiki Reference</a>
     */
    private final int defaultPoints;
}
