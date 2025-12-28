package ru.realite.core.api;

import java.util.Objects;

/**
 * Идентификатор модуля (value object).
 */
public record ModuleId(String value) {

    public ModuleId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ModuleId is blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
