package dinkplugin.domain;

import dinkplugin.notifiers.CollectionNotifier;
import dinkplugin.util.Utils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
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

    public int getClogRankThreshold(Client client) {
        if (this == GILDED) {
            return 25 * (int) (0.9 * client.getVarpValue(CollectionNotifier.TOTAL_POSSIBLE_LOGS_VARP) / 25);
        }
        return client.getStructComposition(this.structId).getIntValue(THRESHOLD_PARAM);
    }

}
