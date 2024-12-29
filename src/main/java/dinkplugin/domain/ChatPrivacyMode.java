package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChatPrivacyMode {
    HIDE_NONE("Show All"),
    HIDE_SPLIT_PM("Hide Split PMs"),
    HIDE_ALL("Hide All");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
