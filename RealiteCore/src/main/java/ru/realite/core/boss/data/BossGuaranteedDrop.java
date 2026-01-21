package ru.realite.core.boss.data;

public record BossGuaranteedDrop(String itemId, int amount) {
    public BossGuaranteedDrop {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId is blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
