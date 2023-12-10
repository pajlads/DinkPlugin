package dinkplugin.domain;

/**
 * Designates whether deaths involve loss of items.
 */
public enum Danger {
    SAFE,
    DANGEROUS,
    EXCEPTIONAL // safe deaths that should be treated as special (e.g., bypass deathIgnoreSafe)
}
