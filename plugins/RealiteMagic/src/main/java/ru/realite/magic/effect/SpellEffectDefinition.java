package ru.realite.magic.effect;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record SpellEffectDefinition(String type, Map<String, Object> params) {

    public SpellEffectDefinition {
        Objects.requireNonNull(type, "type");
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Effect type cannot be blank");
        }
        type = normalized;
        if (params == null || params.isEmpty()) {
            params = Map.of();
        } else {
            params = Collections.unmodifiableMap(new HashMap<>(params));
        }
    }
}
