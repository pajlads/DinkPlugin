package dinkplugin.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ConfigImportPolicy {
    OVERWRITE_WEBHOOKS("Overwrite Webhooks"),
    OVERWRITE_ITEM_LISTS("Overwrite Item Lists");

    private final String displayName;

    @Override
    public String toString() {
        return this.displayName;
    }
}
