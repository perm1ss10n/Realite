package ru.realite.core.boss.data;

public record BossPhaseDefinition(String id, double enterAt) {
    public BossPhaseDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is blank");
        }
    }
}
