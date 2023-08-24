package dinkplugin.domain;

public enum PriceType {
    GrandExchange("Grand Exchange"),

    None("None");

    private final String name;

    PriceType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
