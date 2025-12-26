package ru.realite.classes.model;

public enum HudMode {
    BOSSBAR,
    ACTIONBAR,
    SIDEBAR,
    OFF;

    public static HudMode fromString(String s) {
        if (s == null) return BOSSBAR;
        try {
            return HudMode.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return BOSSBAR;
        }
    }
}
