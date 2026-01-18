package ru.realite.core.api.models;

import java.util.Objects;

public record ApplyResult(ApplyState state, String message) {

    public ApplyResult {
        Objects.requireNonNull(state, "state");
        // Нормализуем message в пустую строку, чтобы наружу никогда не утекал null
        message = (message == null) ? "" : message;
    }

    // Instance-проверки: читаемо и без конфликта с фабриками
    public boolean isApplied() {
        return state == ApplyState.APPLIED;
    }

    public boolean isFallback() {
        return state == ApplyState.FALLBACK;
    }

    public boolean isFailed() {
        return state == ApplyState.FAILED;
    }

    // Static factory methods
    public static ApplyResult applied() {
        return new ApplyResult(ApplyState.APPLIED, "");
    }

    public static ApplyResult applied(String message) {
        return new ApplyResult(ApplyState.APPLIED, message);
    }

    public static ApplyResult fallback() {
        return fallback("");
    }

    public static ApplyResult fallback(String message) {
        return new ApplyResult(ApplyState.FALLBACK, message);
    }

    public static ApplyResult failed() {
        return failed("");
    }

    public static ApplyResult failed(String message) {
        return new ApplyResult(ApplyState.FAILED, message);
    }

}
