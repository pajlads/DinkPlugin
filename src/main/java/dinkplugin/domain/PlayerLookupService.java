package dinkplugin.domain;

public enum PlayerLookupService {
    NONE("None"),
    OSRS_HISCORE("OSRS HiScore"),
    CRYSTAL_MATH_LABS("Crystal Math Labs"),
    TEMPLEOSRS("Temple OSRS"),
    WISEOLDMAN("Wise Old Man");

    private final String name;

    PlayerLookupService(String name) {
        this.name = name;
    }

    public String playerUrl(String playerName) {
        switch (this) {
            case OSRS_HISCORE:
                return "https://secure.runescape.com/m=hiscore_oldschool/hiscorepersonal.ws?user1=" + playerName;
            case WISEOLDMAN:
                return "https://wiseoldman.net/players/" + playerName;
            case CRYSTAL_MATH_LABS:
                return "https://crystalmathlabs.com/track.php?player=" + playerName;
            case TEMPLEOSRS:
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
