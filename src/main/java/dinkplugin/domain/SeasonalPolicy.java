package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SeasonalPolicy {
    ACCEPT("Notify normally"),
    FORWARD_TO_LEAGUES("Use Leagues URL"),
    REJECT("Off");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
