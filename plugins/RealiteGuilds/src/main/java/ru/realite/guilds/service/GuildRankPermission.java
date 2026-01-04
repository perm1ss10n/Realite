package ru.realite.guilds.service;

import java.util.Locale;
import java.util.Optional;

public enum GuildRankPermission {
    INVITE,
    KICK,
    PROMOTE,
    SETHOME,
    HOME,
    CLAIM,
    ACCESS,
    CHAT,
    TP,
    SALARY_VIEW,
    TREASURY_SPEND,
    UPGRADES_MANAGE;

    public static Optional<GuildRankPermission> fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(GuildRankPermission.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
