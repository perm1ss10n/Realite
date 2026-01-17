package ru.realite.familiars.config;

import java.util.List;
import java.util.Objects;

public class ConfigValidationException extends Exception {
    private final List<String> errors;

    public ConfigValidationException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    public List<String> errors() {
        return errors;
    }
}
