package ru.realite.city.model;

public enum PlotType {
    HOME,
    SHOP;

    public static PlotType fromToken(String token) {
        if (token == null) {
            return null;
        }
        return switch (token.toLowerCase()) {
            case "home" -> HOME;
            case "shop" -> SHOP;
            default -> null;
        };
    }

    public String displayName() {
        return name().toLowerCase();
    }
}
