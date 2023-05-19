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

    public static AccountType get(int varbitValue) {
        if (varbitValue < 0 || varbitValue > TYPES.length) return null;
        return TYPES[varbitValue];
    }
}
