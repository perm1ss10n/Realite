package ru.realite.magic.spell;

import java.util.Map;

public record SpellLoadError(String fileName,
                             String spellId,
                             String messageKey,
                             Map<String, String> placeholders,
                             String message) {

    public SpellLoadError {
        if (placeholders == null) {
            placeholders = Map.of();
        }
    }

    public static SpellLoadError ofKey(String fileName,
                                       String spellId,
                                       String messageKey,
                                       Map<String, String> placeholders) {
        return new SpellLoadError(fileName, spellId, messageKey, placeholders, null);
    }

    public static SpellLoadError ofMessage(String fileName, String spellId, String message) {
        return new SpellLoadError(fileName, spellId, null, Map.of(), message);
    }
}
