package ru.realite.magic.cast;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public final class InstantCastStrategy implements CastStrategy {

    @Override
    public CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget) {
        Location origin = caster.getLocation();
        LivingEntity primary = CastTargetUtils.primaryTarget(baseTarget, caster);
        Location impact = CastTargetUtils.impactLocation(baseTarget, caster);
        List<LivingEntity> targets = primary == null ? List.of() : List.of(primary);
        return new CastExecutionPlan(spell, caster, targets, origin, impact, primary, null);
    }
}
