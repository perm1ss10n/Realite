package ru.realite.classes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Components {
    private Components() {}

    // Поддержка '&' кодов: &a &6 и т.д.
    private static final LegacyComponentSerializer LEGACY_AMP =
            LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION =
            LegacyComponentSerializer.legacySection();

    public static Component c(String legacyWithAmpersand) {
        if (legacyWithAmpersand == null) return Component.empty();
        return LEGACY_AMP.deserialize(legacyWithAmpersand);
    }

    public static Component cSection(String legacyWithSection) {
        if (legacyWithSection == null) return Component.empty();
        return LEGACY_SECTION.deserialize(legacyWithSection);
    }
}
