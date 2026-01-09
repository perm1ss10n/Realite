package ru.realite.magic.validation;

import java.util.Map;

public record SchemaError(String file,
                          String spellId,
                          String path,
                          String messageKey,
                          Map<String, String> placeholders) {
}
