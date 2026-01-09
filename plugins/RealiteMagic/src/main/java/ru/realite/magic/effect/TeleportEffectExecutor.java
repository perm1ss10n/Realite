package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import ru.realite.magic.pve.PveService;

public final class TeleportEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "teleport";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object targetRaw = params.get("target");
        EffectTargetType targetType = EffectTargetType.from(targetRaw);
        if (targetRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "target"));
        }
        if (targetType != EffectTargetType.IMPACT
                && targetType != EffectTargetType.ORIGIN
                && targetType != EffectTargetType.PRIMARY
                && targetType != EffectTargetType.LOCATION) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "target", "value", String.valueOf(targetRaw)));
        }
        Object safeRaw = params.get("safe");
        if (safeRaw != null && EffectParamUtils.booleanParam(params, "safe") == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "safe", "value", String.valueOf(safeRaw)));
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        if (ctx == null || def == null) {
            return;
        }
        Map<String, Object> params = def.params();
        EffectTargetType targetType = EffectTargetType.from(params.get("target"));
        Boolean safe = EffectParamUtils.booleanParam(params, "safe");
        boolean safeTeleport = safe != null && safe;
        if (targetType == null) {
            return;
        }
        if (ctx.plan() != null && ctx.plan().primaryTarget() != null) {
            if (targetType == EffectTargetType.PRIMARY
                    || targetType == EffectTargetType.ENTITY
                    || targetType == EffectTargetType.LOCATION) {
                PveService pveService = ctx.magicService().pveService();
                if (pveService.isTeleportImmune(ctx.plan().primaryTarget())) {
                    ctx.magicService().diagnosticsService().recordPveImmune(def.type());
                    return;
                }
            }
        }
        Location target = EffectTargetResolver.resolveLocation(ctx.plan(), targetType, ctx.caster());
        if (target == null) {
            return;
        }
        Location destination = safeTeleport ? findSafeLocation(target) : target;
        Player caster = ctx.caster();
        if (destination == null) {
            caster.sendMessage(ctx.magicService().messages().msg("magic.teleport.failed"));
            return;
        }
        caster.teleport(destination);
    }

    private Location findSafeLocation(Location base) {
        if (base == null) {
            return null;
        }
        World world = base.getWorld();
        if (world == null) {
            return null;
        }
        Location candidate = base.clone();
        if (isSafe(candidate)) {
            return candidate;
        }
        Location highest = world.getHighestBlockAt(base).getLocation().add(0, 1, 0);
        if (isSafe(highest)) {
            return highest;
        }
        return null;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);
        if (feet.isLiquid() || head.isLiquid()) {
            return false;
        }
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }
        if (!ground.getType().isSolid() || ground.isLiquid()) {
            return false;
        }
        return true;
    }
}
