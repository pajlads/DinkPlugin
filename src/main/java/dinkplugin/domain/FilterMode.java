package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum FilterMode {
    ALLOW("Exclusively Allow"),
    DENY("Selectively Deny");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
