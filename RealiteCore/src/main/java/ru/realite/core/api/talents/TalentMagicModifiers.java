package ru.realite.core.api.talents;

public record TalentMagicModifiers(double damageMultiplier,
                                   double manaMultiplier,
                                   double cooldownMultiplier) {
    public static TalentMagicModifiers identity() {
        return new TalentMagicModifiers(1.0, 1.0, 1.0);
    }
}
