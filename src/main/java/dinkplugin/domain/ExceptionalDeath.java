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
    JAD_CHALLENGES("Jad challenges"),
    TOB("Theatre of Blood"),
    TOA("Tombs of Amascut");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
