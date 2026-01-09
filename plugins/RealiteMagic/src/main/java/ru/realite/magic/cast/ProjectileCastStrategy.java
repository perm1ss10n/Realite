package ru.realite.magic.cast;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;
import ru.realite.magic.target.SpellTargetDefinition;

public final class ProjectileCastStrategy implements CastStrategy {

    private final CastLimits limits;

    public ProjectileCastStrategy(CastLimits limits) {
        this.limits = limits;
    }

    @Override
    public CastExecutionPlan execute(Player caster, SpellDefinition spell, SpellTarget baseTarget) {
        ProjectileCastDefinition definition = spell.projectileCast();
        if (definition == null) {
            return new InstantCastStrategy().execute(caster, spell, baseTarget);
        }
        Location origin = caster.getLocation();
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        double speed = Math.max(0.1, definition.speed());
        double maxDistance = Math.min(definition.maxDistance(), limits.maxProjectileDistance());
        double hitRadius = Math.max(0.1, definition.hitRadius());
        ProjectileHitPolicy onHit = definition.onHit() == null ? ProjectileHitPolicy.STOP : definition.onHit();
        List<LivingEntity> targets = new ArrayList<>();
        LivingEntity primary = null;
        SpellTargetDefinition targetDefinition = spell.target();
        World world = eye.getWorld();
        if (world == null) {
            return new CastExecutionPlan(spell, caster, List.of(), origin, null, null, null);
        }
        Location current = eye.clone();
        Vector velocity = direction.clone().multiply(speed);
        double traveled = 0.0;
        Location impact = null;
        while (traveled <= maxDistance) {
            Location next = current.clone().add(velocity);
            RayTraceResult blockHit = world.rayTraceBlocks(current, velocity.clone().normalize(), velocity.length());
            if (blockHit != null && blockHit.getHitPosition() != null) {
                impact = blockHit.getHitPosition().toLocation(world);
                break;
            }
            LivingEntity hit = CastTraceUtils.findEntityHit(world, next, hitRadius, caster, targetDefinition);
            if (hit != null) {
                if (primary == null) {
                    primary = hit;
                }
                if (!targets.contains(hit)) {
                    targets.add(hit);
                }
                if (onHit == ProjectileHitPolicy.STOP) {
                    impact = hit.getLocation();
                    break;
                }
            }
            current = next;
            traveled += velocity.length();
            if (definition.gravity()) {
                velocity = velocity.clone().add(new Vector(0, -0.05, 0));
            }
        }
        if (impact == null) {
            impact = current;
        }
        return new CastExecutionPlan(spell, caster, List.copyOf(targets), origin, impact, primary, null);
    }
}
