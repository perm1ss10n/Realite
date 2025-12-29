package ru.realite.city.model;

public enum PlotMemberRole {
    MEMBER,
    TRUSTED;

    public static PlotMemberRole fromToken(String token) {
        if (token == null) {
            return null;
        }
        return switch (token.toLowerCase()) {
            case "member" -> MEMBER;
            case "trusted" -> TRUSTED;
            default -> null;
        };
    }
}
