package dinkplugin.domain;

import dinkplugin.util.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import org.jetbrains.annotations.VisibleForTesting;

@RequiredArgsConstructor
public enum CollectionLogRank {
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

    @VisibleForTesting
    public static final int THRESHOLD_PARAM = 2232;

    @Getter(onMethod_ = { @VisibleForTesting })
    private final int structId;
    private final String displayName = Utils.ucFirst(this.name());

    @Override
    public String toString() {
        return this.displayName;
    }

    /**
     * @param client {@link Client}
     * @return the number of collection log entries needed to unlock this rank
     */
    public int getClogRankThreshold(Client client) {
        assert client.isClientThread();
        switch (this) {
            case NONE:
                return 0;

            case GILDED:
                // https://oldschool.runescape.wiki/w/Gilded_staff_of_collection
                return 25 * (int) (0.9 * client.getVarpValue(VarPlayerID.COLLECTION_COUNT_MAX) / 25);

            default:
                return client.getStructComposition(this.structId).getIntValue(THRESHOLD_PARAM);
        }
    }

}
