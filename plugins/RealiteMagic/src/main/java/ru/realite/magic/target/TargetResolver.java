package ru.realite.magic.target;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import ru.realite.magic.spell.SpellDefinition;

public final class TargetResolver {

    public Optional<SpellTarget> resolve(Player caster, SpellDefinition spell) {
        if (caster == null || spell == null) {
            return Optional.empty();
        }
        SpellTargetDefinition target = spell.target();
        if (target == null || target.type() == null) {
            return Optional.empty();
        }
        return switch (target.type()) {
            case NONE -> Optional.empty();
            case SELF -> Optional.of(new SpellTarget.Self(caster));
            case ENTITY -> resolveEntity(caster, spell, target);
            case BLOCK -> resolveBlock(caster, spell, target);
            case LOCATION -> resolveLocation(caster, spell, target);
        };
    }

    private Optional<SpellTarget> resolveEntity(Player caster,
                                                SpellDefinition spell,
                                                SpellTargetDefinition target) {
        double maxDistance = resolveDistance(spell, target);
        if (maxDistance <= 0) {
            return Optional.empty();
        }
        Location eye = caster.getEyeLocation();
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                eye,
                eye.getDirection(),
                maxDistance,
                0.2,
                entity -> isAllowedEntity(entity, caster, target));
        if (result == null) {
            return Optional.empty();
        }
        Entity hit = result.getHitEntity();
        if (!(hit instanceof LivingEntity living)) {
            return Optional.empty();
        }
        if (target.lineOfSight() && !caster.hasLineOfSight(hit)) {
            return Optional.empty();
        }
        return Optional.of(new SpellTarget.EntityTarget(living));
    }

    private Optional<SpellTarget> resolveBlock(Player caster,
                                               SpellDefinition spell,
                                               SpellTargetDefinition target) {
        double maxDistance = resolveDistance(spell, target);
        if (maxDistance <= 0) {
            return Optional.empty();
        }
        RayTraceResult result = caster.rayTraceBlocks(maxDistance, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null) {
            return Optional.empty();
        }
        Block block = Objects.requireNonNull(result.getHitBlock());
        Location location = block.getLocation();
        return Optional.of(new SpellTarget.BlockTarget(block, location));
    }

    private Optional<SpellTarget> resolveLocation(Player caster,
                                                  SpellDefinition spell,
                                                  SpellTargetDefinition target) {
        double maxDistance = resolveDistance(spell, target);
        if (maxDistance <= 0) {
            return Optional.empty();
        }
        RayTraceResult result = caster.rayTraceBlocks(maxDistance, FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null) {
            return Optional.empty();
        }
        Location location = result.getHitPosition() != null
                ? result.getHitPosition().toLocation(caster.getWorld())
                : result.getHitBlock().getLocation();
        return Optional.of(new SpellTarget.LocationTarget(location));
    }

    private double resolveDistance(SpellDefinition spell, SpellTargetDefinition target) {
        double fallback = spell.range();
        return target.effectiveDistance(fallback);
    }

    private boolean isAllowedEntity(Entity entity, Player caster, SpellTargetDefinition target) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (living.getUniqueId().equals(caster.getUniqueId())) {
            return false;
        }
        if (living instanceof Player) {
            return target.allowPlayers();
        }
        return target.allowMobs();
    }
}
