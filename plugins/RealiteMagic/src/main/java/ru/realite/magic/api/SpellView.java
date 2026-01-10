package ru.realite.magic.api;

import java.util.List;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellRequirements;
import ru.realite.magic.spell.SpellType;
import ru.realite.magic.target.SpellTargetDefinition;

public record SpellView(
        String id,
        String nameKey,
        String descriptionKey,
        SpellType type,
        MagicSchool school,
        SpellRequirements requirements,
        SpellTargetDefinition target,
        List<SpellEffectDefinition> effects
) {
}
