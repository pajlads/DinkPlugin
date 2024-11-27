package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SeasonalPolicy {
    ACCEPT("Notify normally"),
    REJECT("Drop notification"),
    FORWARD_TO_LEAGUES("Use Leagues URL");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
