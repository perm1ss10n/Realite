package ru.realite.magic.effect;

import java.util.Map;

public record EffectValidationResult(boolean valid, String messageKey, Map<String, String> placeholders) {

    public static EffectValidationResult ok() {
        return new EffectValidationResult(true, null, Map.of());
    }

    public static EffectValidationResult fail(String messageKey, Map<String, String> placeholders) {
        return new EffectValidationResult(false, messageKey, placeholders == null ? Map.of() : Map.copyOf(placeholders));
    }

    public boolean isValid() {
        return valid;
    }
}
