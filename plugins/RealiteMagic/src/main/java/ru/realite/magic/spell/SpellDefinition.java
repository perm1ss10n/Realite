package ru.realite.magic.spell;

import java.util.List;
import org.bukkit.Material;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.target.SpellTargetDefinition;

public record SpellDefinition(
        String id,
        SpellType type,
        String nameKey,
        String descKey,
        MagicSchool school,
        double mana,
        long cooldownTicks,
        double range,
        double damage,
        SpellRequirements requirements,
        SpellTargetDefinition target,
        List<SpellEffectDefinition> effects,
        SpellCastTrigger castTrigger,
        String castItemId,
        Integer staffChargesCost,
        String giveItemId,
        int giveItemAmount,
        Material iconMaterial,
        Integer iconCustomModelData,
        Integer guiSlot
) {}
