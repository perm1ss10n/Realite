package ru.realite.magic.spell;

import org.bukkit.Material;

public record SpellDefinition(
        String id,
        SpellType type,
        String nameKey,
        String descKey,
        double mana,
        long cooldownTicks,
        double range,
        double damage,
        SpellRequirements requirements,
        String castItemId,
        String giveItemId,
        int giveItemAmount,
        Material iconMaterial,
        Integer iconCustomModelData,
        Integer guiSlot
) {}
