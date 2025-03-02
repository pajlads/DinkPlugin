package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ExternalScreenshotPolicy {
    ALWAYS("Always"),
    REQUESTED("When requested"),
    NEVER("Never");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
