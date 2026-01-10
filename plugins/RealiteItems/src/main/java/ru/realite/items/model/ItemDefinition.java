package ru.realite.items.model;

import org.bukkit.Material;

import java.util.List;

public record ItemDefinition(
        String id,
        Material material,
        Integer customModelData,
        String nameKey,
        List<String> loreKeys,
        boolean glow,
        boolean unstackable
) {}
