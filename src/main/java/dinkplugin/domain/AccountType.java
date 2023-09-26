package dinkplugin.domain;

public enum AccountType {
    NORMAL,
    IRONMAN,
    ULTIMATE_IRONMAN,
    HARDCORE_IRONMAN,
    GROUP_IRONMAN,
    HARDCORE_GROUP_IRONMAN,
    UNRANKED_GROUP_IRONMAN;

    private static final AccountType[] TYPES = values();

    public boolean isHardcore() {
        return this == HARDCORE_IRONMAN || this == HARDCORE_GROUP_IRONMAN;
    }

    /**
     * @param varbitValue the value associated with {@link net.runelite.api.Varbits#ACCOUNT_TYPE}
     * @return the equivalent enum value
     */
    public static AccountType get(int varbitValue) {
        if (varbitValue < 0 || varbitValue >= TYPES.length) return null;
        return TYPES[varbitValue];
    }
}
