package ru.realite.magic.cast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public final class AoeCastStrategy implements CastStrategy {

    private final CastLimits limits;

    public AoeCastStrategy(CastLimits limits) {
        this.limits = limits;
    }

    @Override
    public CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget) {
        AoeCastDefinition definition = spell.aoeCast();
        if (definition == null) {
            return new InstantCastStrategy().execute(caster, spell, baseTarget);
        }
        Location origin = caster.getLocation();
        Location center = CastTargetUtils.impactLocation(baseTarget, caster);
        double radius = Math.max(0.0, definition.radius());
        int maxTargets = Math.min(Math.max(1, definition.maxTargets()), limits.maxAoeTargets());
        List<LivingEntity> targets = resolveTargets(caster, center, radius, definition, maxTargets);
        LivingEntity primary = targets.isEmpty()
                ? CastTargetUtils.primaryTarget(baseTarget, caster)
                : targets.get(0);
        spawnIndicator(center, radius);
        return new CastExecutionPlan(spell, caster, targets, origin, center, primary, null);
    }

    private List<LivingEntity> resolveTargets(Player caster,
                                              Location center,
                                              double radius,
                                              AoeCastDefinition definition,
                                              int maxTargets) {
        if (center == null) {
            return List.of();
        }
        World world = center.getWorld();
        if (world == null) {
            return List.of();
        }
        List<LivingEntity> found = new ArrayList<>();
        for (var entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!CastTargetFilter.isAllowedEntity(living, caster, definition.includePlayers(), definition.includeMobs())) {
                continue;
            }
            found.add(living);
        }
        found.sort(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)));
        if (found.size() > maxTargets) {
            return List.copyOf(found.subList(0, maxTargets));
        }
        return List.copyOf(found);
    }

    private void spawnIndicator(Location center, double radius) {
        if (center == null) {
            return;
        }
        World world = center.getWorld();
        if (world == null || radius <= 0) {
            return;
        }
        int points = 24;
        double step = (Math.PI * 2) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location particleLocation = new Location(world, x, center.getY() + 0.1, z);
            world.spawnParticle(Particle.SPELL_WITCH, particleLocation, 1, 0, 0, 0, 0);
        }
    }
}
