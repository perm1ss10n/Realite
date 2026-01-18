package ru.realite.core.api.models;

import java.util.Objects;

public record ApplyResult(ApplyState state, String message) {

    public ApplyResult {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(message, "message");
    }

    public boolean applied() {
        return state == ApplyState.APPLIED;
    }

    public boolean fallback() {
        return state == ApplyState.FALLBACK;
    }

    public boolean failed() {
        return state == ApplyState.FAILED;
    }

    public static ApplyResult applied() {
        return new ApplyResult(ApplyState.APPLIED, "");
    }

    public static ApplyResult applied(String message) {
        return new ApplyResult(ApplyState.APPLIED, message == null ? "" : message);
    }

    public static ApplyResult fallback(String message) {
        return new ApplyResult(ApplyState.FALLBACK, message == null ? "" : message);
    }

    public static ApplyResult failed(String message) {
        return new ApplyResult(ApplyState.FAILED, message == null ? "" : message);
    }
}
