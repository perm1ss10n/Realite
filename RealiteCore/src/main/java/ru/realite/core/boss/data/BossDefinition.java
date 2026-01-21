package ru.realite.core.boss.data;

import org.bukkit.entity.EntityType;

import java.util.List;

public record BossDefinition(
        String id,
        String name,
        EntityType entityType,
        double maxHp,
        List<BossPhaseDefinition> phases,
        List<String> abilityIds
) {
    public BossDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is blank");
        }
        if (entityType == null) {
            throw new IllegalArgumentException("entityType is null");
        }
        if (maxHp <= 0.0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }
        phases = phases == null ? List.of() : List.copyOf(phases);
        abilityIds = abilityIds == null ? List.of() : List.copyOf(abilityIds);
    }
}
