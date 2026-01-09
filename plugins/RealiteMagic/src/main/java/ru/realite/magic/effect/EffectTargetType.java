package ru.realite.magic.effect;

import java.util.Locale;

public enum EffectTargetType {
    ENTITY,
    LOCATION;

    public static EffectTargetType from(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return EffectTargetType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
