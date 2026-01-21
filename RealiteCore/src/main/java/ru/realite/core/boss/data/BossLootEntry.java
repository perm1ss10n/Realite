package ru.realite.core.boss.data;

public record BossLootEntry(String itemId, int min, int max, int weight) {
    public BossLootEntry {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is blank");
        }
        if (min <= 0 || max <= 0) {
            throw new IllegalArgumentException("min/max must be positive");
        }
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be positive");
        }
    }
}
