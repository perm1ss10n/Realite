package ru.realite.classes.model;

public enum ClassId {
    WANDERER,
    WARRIOR,
    ARCHER,
    MINER,
    ALCHEMIST,
    MERCHANT;

    public static ClassId fromString(String s) {
        if (s == null) return null;
        try {
            return ClassId.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
