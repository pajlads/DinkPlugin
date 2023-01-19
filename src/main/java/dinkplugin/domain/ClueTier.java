package dinkplugin.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ClueTier {
    BEGINNER,
    EASY,
    MEDIUM,
    HARD,
    ELITE,
    MASTER;

    private static final Map<String, ClueTier> MAPPINGS;
    private final String displayName = this.name().charAt(0) + this.name().substring(1).toLowerCase();

    @Override
    public String toString() {
        return this.displayName;
    }

    @Nullable
    public static ClueTier parse(@NotNull String tier) {
        return MAPPINGS.get(tier.toUpperCase());
    }

    static {
        MAPPINGS = Arrays.stream(values()).collect(Collectors.toMap(Enum::name, Function.identity()));
    }
}
