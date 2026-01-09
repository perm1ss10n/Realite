package ru.realite.magic.effect;

public record BalanceModifiers(double damageMultiplier,
                               double manaMultiplier,
                               double cooldownMultiplier) {

    public static BalanceModifiers identity() {
        return new BalanceModifiers(1.0, 1.0, 1.0);
    }
}
