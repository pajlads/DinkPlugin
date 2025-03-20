package dinkplugin.domain;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.StructComposition;

@Getter
@RequiredArgsConstructor
public enum CollectionLogRanks {
    UNKNOWN(0),
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

    public void initialize(StructComposition struct) {
        if (struct != null) {
            this.rankName = struct.getStringValue(2231).replaceAll(".*?<col=[^>]+>", "").trim();
            this.clogRankThreshold = struct.getIntValue(2232);
        }
    }

    public void setClogRankThreshold(int threshold) {
        this.clogRankThreshold = threshold;
    }

    public int getClogRankThreshold() {
        return clogRankThreshold;
    }
}
