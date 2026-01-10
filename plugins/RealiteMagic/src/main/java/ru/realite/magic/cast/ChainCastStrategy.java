package ru.realite.magic.cast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public final class ChainCastStrategy implements CastStrategy {

    private final CastLimits limits;

    public ChainCastStrategy(CastLimits limits) {
        this.limits = limits;
    }

    @Override
    public CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget) {
        ChainCastDefinition definition = spell.chainCast();
        if (definition == null) {
            return new InstantCastStrategy().execute(caster, spell, baseTarget);
        }
        Location origin = caster.getLocation();
        LivingEntity starting = CastTargetUtils.primaryTarget(baseTarget, caster);
        if (starting == null) {
            return new CastExecutionPlan(spell, caster, List.of(), origin, null, null, null);
        }
        int maxTargets = Math.min(definition.jumps() + 1, limits.maxChainTargets());
        List<LivingEntity> targets = new ArrayList<>();
        Set<LivingEntity> visited = new HashSet<>();
        LivingEntity current = starting;
        while (current != null && targets.size() < maxTargets) {
            if (!visited.contains(current)) {
                visited.add(current);
                if (CastTargetFilter.isAllowedEntity(current, caster,
                        definition.includePlayers(), definition.includeMobs())) {
                    targets.add(current);
                }
            }
            LivingEntity next = findNextTarget(current, caster, definition.jumpRange(), visited, definition);
            current = next;
        }
        LivingEntity primary = targets.isEmpty() ? starting : targets.get(0);
        Location impact = primary == null ? null : primary.getLocation();
        return new CastExecutionPlan(spell, caster, List.copyOf(targets), origin, impact, primary, null);
    }

    private LivingEntity findNextTarget(LivingEntity from,
                                        Player caster,
                                        double range,
                                        Set<LivingEntity> visited,
                                        ChainCastDefinition definition) {
        World world = from.getWorld();
        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (var entity : world.getNearbyEntities(from.getLocation(), range, range, range)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (visited.contains(living)) {
                continue;
            }
            if (!CastTargetFilter.isAllowedEntity(living, caster,
                    definition.includePlayers(), definition.includeMobs())) {
                continue;
            }
            double distance = living.getLocation().distanceSquared(from.getLocation());
            if (distance < closestDist) {
                closestDist = distance;
                closest = living;
            }
        }
        return closest;
    }
}
