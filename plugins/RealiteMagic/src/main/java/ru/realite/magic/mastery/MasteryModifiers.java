package ru.realite.magic.mastery;

public record MasteryModifiers(double damageMultiplier,
                               double manaMultiplier,
                               double cooldownMultiplier) {

    public static MasteryModifiers identity() {
        return new MasteryModifiers(1.0, 1.0, 1.0);
    }
}
