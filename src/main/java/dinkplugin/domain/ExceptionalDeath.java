package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

/**
 * Safe deaths that can be customized to trigger death notifications even in dangerous-only mode.
 */
@RequiredArgsConstructor
public enum ExceptionalDeath {
    COX("Chambers of Xeric"),
    FIGHT_CAVE("Fight Caves"),
    INFERNO("Inferno"),
    TOA("Tombs of Amascut"),
    JAD_CHALLENGES("Jad challenges");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
