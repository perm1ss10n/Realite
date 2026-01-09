package ru.realite.magic.cast;

import org.bukkit.entity.Player;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public interface CastStrategy {
    CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget);
}
