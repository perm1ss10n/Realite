package ru.realite.classes.model;

public enum ClassId {
    WANDERER,
    WARRIOR,
    ARCHER,
    MINER,
    ALCHEMIST,
    MERCHANT,
    MERCENARY, //Чтобы было видно скрытые классы, возможно и не нужно будет
    WARLOCK;

    public static ClassId fromString(String s) {
        if (s == null) return null;
        try {
            return ClassId.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
