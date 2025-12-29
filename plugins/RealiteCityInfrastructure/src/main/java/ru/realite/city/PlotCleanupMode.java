package ru.realite.city;

public enum PlotCleanupMode {
    AIR_ONLY,
    FLAT;

    public static PlotCleanupMode fromToken(String token) {
        if (token == null) {
            return AIR_ONLY;
        }
        return switch (token.toUpperCase()) {
            case "FLAT" -> FLAT;
            default -> AIR_ONLY;
        };
    }
}
