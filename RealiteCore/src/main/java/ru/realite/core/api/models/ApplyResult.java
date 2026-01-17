package ru.realite.core.api.models;

import java.util.Objects;

public record ApplyResult(boolean success, String message) {

    public ApplyResult {
        Objects.requireNonNull(message, "message");
    }

    public static ApplyResult ok() {
        return new ApplyResult(true, "");
    }

    public static ApplyResult ok(String message) {
        return new ApplyResult(true, message == null ? "" : message);
    }

    public static ApplyResult fail(String message) {
        return new ApplyResult(false, message == null ? "" : message);
    }
}
