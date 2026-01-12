package ru.realite.core.api.ui;

import java.util.Objects;

/**
 * Идентификатор UI-провайдера (value object).
 */
public record UiProviderId(String value) {

    public UiProviderId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("UiProviderId is blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
