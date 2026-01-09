package ru.realite.magic.effect;

import java.util.Locale;

public enum EffectApplyMode {
    PRIMARY,
    ALL;

    public static EffectApplyMode from(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return EffectApplyMode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
