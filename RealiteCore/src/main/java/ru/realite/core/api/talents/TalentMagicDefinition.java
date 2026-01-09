package ru.realite.core.api.talents;

public record TalentMagicDefinition(String school,
                                   String delivery,
                                   String effect,
                                   TalentMagicModifiers modifiers,
                                   TalentMagicOnDamage onDamage) {
}
