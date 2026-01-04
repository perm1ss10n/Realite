package ru.realite.guilds.model.upgrade;

import java.util.List;
import java.util.Map;

public record UpgradeDefinition(
        String id,
        boolean enabled,
        String name,
        String description,
        int maxLevel,
        UpgradeCost cost,
        Map<String, Integer> requirements,
        List<UpgradeEffect> effects) {
}
