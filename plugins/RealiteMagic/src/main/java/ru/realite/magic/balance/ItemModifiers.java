package ru.realite.magic.balance;

public record ItemModifiers(double damageMultiplier,
                            double manaMultiplier,
                            double cooldownMultiplier) {

    public static ItemModifiers identity() {
        return new ItemModifiers(1.0, 1.0, 1.0);
    }
}
