package ru.realite.magic.service;

import java.util.Locale;

public enum MagicConfigSection {
    ALL,
    HUD,
    SCHOOLS,
    MASTERY,
    PVE,
    REGIONS,
    REAGENTS,
    ECONOMY,
    LIMITS,
    BALANCE;

    public static MagicConfigSection fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return MagicConfigSection.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
