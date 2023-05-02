package dinkplugin.domain;

import com.google.common.net.UrlEscapers;

public enum PlayerLookupService {
    NONE("None"),
    OSRS_HISCORE("OSRS HiScore"),
    CRYSTAL_MATH_LABS("Crystal Math Labs"),
    TEMPLE_OSRS("Temple OSRS"),
    WISE_OLD_MAN("Wise Old Man"),
    COLLECTION_LOG("CollectionLog.net");

    private final String name;

    PlayerLookupService(String name) {
        this.name = name;
    }

    public String getPlayerUrl(String playerName) {
        String escapedName = UrlEscapers.urlPathSegmentEscaper().escape(playerName);
        switch (this) {
            case OSRS_HISCORE:
                return "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=" + escapedName;
            case WISE_OLD_MAN:
                return "https://wiseoldman.net/players/" + escapedName;
            case CRYSTAL_MATH_LABS:
                return "https://crystalmathlabs.com/track.php?player=" + escapedName;
            case TEMPLE_OSRS:
                return "https://templeosrs.com/player/overview.php?player=" + escapedName;
            case COLLECTION_LOG:
                return "https://collectionlog.net/log/" + escapedName;
            case NONE:
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
