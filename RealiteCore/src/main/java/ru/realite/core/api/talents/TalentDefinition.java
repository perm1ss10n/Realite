package ru.realite.core.api.talents;

public record TalentDefinition(String id,
                               String nameKey,
                               String descriptionKey,
                               TalentRequirements requirements,
                               TalentMagicDefinition magic) {
}
