package ru.realite.magic.spell;

public record SpellDefinition(
        String id,
        SpellType type,
        String nameKey,
        String descKey,
        double mana,
        long cooldownTicks,
        double range,
        double damage
) {}
