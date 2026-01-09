package ru.realite.items.model;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record ItemDefinition(
        String id,
        Material material,
        Integer customModelData,
        String nameKey,
        List<String> loreKeys,
        boolean glow,
        boolean unstackable,
        Map<String, Object> tags
) {}
