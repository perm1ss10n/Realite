package ru.realite.magic.school;

public record SchoolModifiers(double damageMultiplier,
                              double manaMultiplier,
                              double cooldownMultiplier) {

    public static SchoolModifiers identity() {
        return new SchoolModifiers(1.0, 1.0, 1.0);
    }
}
