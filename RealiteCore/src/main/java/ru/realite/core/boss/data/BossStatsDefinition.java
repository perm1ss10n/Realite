package ru.realite.core.boss.data;

public record BossStatsDefinition(double maxHp, double baseDamage, double movementSpeed) {
    public BossStatsDefinition {
        if (maxHp <= 0.0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }
        if (baseDamage < 0.0) {
            throw new IllegalArgumentException("baseDamage must be >= 0");
        }
        if (movementSpeed < 0.0) {
            throw new IllegalArgumentException("movementSpeed must be >= 0");
        }
    }
}
