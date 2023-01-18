package dinkplugin.domain;

public enum PlayerLookupService {
    NONE("None"),
    OSRS_HISCORE("OSRS HiScore"),
    CRYSTAL_MATH_LABS("Crystal Math Labs"),
    TEMPLE_OSRS("Temple OSRS"),
    WISE_OLD_MAN("Wise Old Man");

    private final String name;

    PlayerLookupService(String name) {
        this.name = name;
    }

    public String getPlayerUrl(String playerName) {
        switch (this) {
            case OSRS_HISCORE:
                return "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal?user1=" + playerName;
            case WISE_OLD_MAN:
                return "https://wiseoldman.net/players/" + playerName;
            case CRYSTAL_MATH_LABS:
                return "https://crystalmathlabs.com/track.php?player=" + playerName;
            case TEMPLE_OSRS:
                return "https://templeosrs.com/player/overview.php?player=" + playerName;
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
