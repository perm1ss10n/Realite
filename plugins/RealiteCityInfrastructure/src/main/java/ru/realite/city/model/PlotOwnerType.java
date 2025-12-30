package ru.realite.city.model;

public enum PlotOwnerType {
    PLAYER,
    GUILD;

    public static PlotOwnerType fromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return PlotOwnerType.valueOf(token.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
