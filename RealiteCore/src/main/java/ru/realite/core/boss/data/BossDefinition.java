package ru.realite.core.boss.data;

import org.bukkit.entity.EntityType;

import java.util.List;

public record BossDefinition(
        String id,
        String name,
        int tier,
        EntityType entityType,
        BossStatsDefinition stats,
        String modelId,
        BossEquipmentDefinition equipment,
        List<BossPhaseDefinition> phases,
        List<String> abilityIds,
        BossLootDefinition loot,
        int maxActiveInstances
) {
    public BossDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is blank");
        }
        if (entityType == null) {
            throw new IllegalArgumentException("entityType is null");
        }
        if (stats == null) {
            throw new IllegalArgumentException("stats is null");
        }
        if (tier <= 0) {
            throw new IllegalArgumentException("tier must be positive");
        }
        if (maxActiveInstances < 0) {
            throw new IllegalArgumentException("maxActiveInstances must be >= 0");
        }
        phases = phases == null ? List.of() : List.copyOf(phases);
        abilityIds = abilityIds == null ? List.of() : List.copyOf(abilityIds);
        loot = loot == null ? BossLootDefinition.empty() : loot;
        equipment = equipment == null ? BossEquipmentDefinition.empty() : equipment;
    }
}
