package ru.realite.magic.cast;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;
import ru.realite.magic.target.SpellTargetDefinition;

public final class BeamCastStrategy implements CastStrategy {

    private final CastLimits limits;

    public BeamCastStrategy(CastLimits limits) {
        this.limits = limits;
    }

    @Override
    public CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget) {
        BeamCastDefinition definition = spell.beamCast();
        if (definition == null) {
            return new InstantCastStrategy().execute(caster, spell, baseTarget);
        }
        Location origin = caster.getLocation();
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double maxDistance = Math.min(definition.maxDistance(), limits.maxBeamDistance());
        double step = Math.max(0.05, definition.step());
        double hitRadius = Math.max(0.1, definition.hitRadius());
        Location impact = null;
        LivingEntity primary = null;
        SpellTargetDefinition targetDefinition = spell.target();
        World world = eye.getWorld();
        if (world == null) {
            return new CastExecutionPlan(spell, caster, List.of(), origin, null, null, null);
        }
        double blockDistance = maxDistance;
        RayTraceResult blockHit = world.rayTraceBlocks(eye, direction, maxDistance);
        if (blockHit != null && blockHit.getHitPosition() != null) {
            blockDistance = eye.distance(blockHit.getHitPosition().toLocation(world));
        }
        BeamParticlesDefinition particles = definition.particles();
        Particle beamParticle = particles == null ? null : parseParticle(particles.particle());
        int countPerStep = particles == null ? 0 : Math.max(0, particles.countPerStep());
        for (double traveled = 0; traveled <= maxDistance; traveled += step) {
            Location point = eye.clone().add(direction.clone().multiply(traveled));
            if (beamParticle != null && countPerStep > 0) {
                world.spawnParticle(beamParticle, point, countPerStep, 0, 0, 0, 0);
            }
            LivingEntity hit = CastTraceUtils.findEntityHit(world, point, hitRadius, caster, targetDefinition);
            if (hit != null) {
                primary = hit;
                impact = hit.getLocation();
                break;
            }
            if (traveled >= blockDistance) {
                impact = point;
                break;
            }
        }
        List<LivingEntity> targets = primary == null ? List.of() : List.of(primary);
        if (impact == null) {
            impact = eye.clone().add(direction.multiply(maxDistance));
        }
        return new CastExecutionPlan(spell, caster, targets, origin, impact, primary, null);
    }

    private Particle parseParticle(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Particle.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
