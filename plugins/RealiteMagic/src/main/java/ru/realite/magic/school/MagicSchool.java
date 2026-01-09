package ru.realite.magic.school;

public enum MagicSchool {
    NONE,
    WARLOCK,
    FIRE,
    ARCANE,
    FROST,
    HOLY,
    NATURE;

    public static MagicSchool fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MagicSchool.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
