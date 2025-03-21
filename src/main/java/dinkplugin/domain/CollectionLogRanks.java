package dinkplugin.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.StructComposition;
import net.runelite.api.annotations.Varp;
import org.jetbrains.annotations.VisibleForTesting;

@Getter
@RequiredArgsConstructor
public enum CollectionLogRanks {
    NONE(0),
    BRONZE(1714),
    IRON(1715),
    STEEL(1716),
    BLACK(1717),
    MITHRIL(1718),
    ADAMANT(1719),
    RUNE(1740),
    DRAGON(1741),
    GILDED(1742);

    /**
     * Collection log rank's structures.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Collection_log">Wiki Reference</a>
     */

    private final int structId;
    private String rankName;
    private int clogRankThreshold;
    @VisibleForTesting
    @Varp
    public static final int RANK_VARP = 2231, RANK_CLOGS_VARP = 2232;


    public void initialize(StructComposition struct) {
        if (struct != null) {
            this.rankName = struct.getStringValue(RANK_VARP).replaceAll(".*?<col=[^>]+>", "").trim();
            this.clogRankThreshold = struct.getIntValue(RANK_CLOGS_VARP);
        }
    }

    public void setClogRankThreshold(int threshold) {
        this.clogRankThreshold = threshold;
    }

    public int getClogRankThreshold() {
        return clogRankThreshold;
    }
}
