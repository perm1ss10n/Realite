package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import ru.realite.magic.pve.PveService;

public final class KnockbackEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "knockback";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object strengthRaw = params.get("strength");
        Double strength = EffectParamUtils.doubleParam(params, "strength");
        if (strengthRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "strength"));
        }
        if (strength == null || strength < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "strength", "value", String.valueOf(strengthRaw)));
        }
        Object modeRaw = params.get("mode");
        if (modeRaw != null && EffectApplyMode.from(modeRaw) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "mode", "value", String.valueOf(modeRaw)));
        }
        Object pullRaw = params.get("pull");
        if (pullRaw != null && EffectParamUtils.booleanParam(params, "pull") == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "pull", "value", String.valueOf(pullRaw)));
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Double strength = EffectParamUtils.doubleParam(params, "strength");
        if (strength == null || strength <= 0) {
            return;
        }
        EffectApplyMode mode = EffectApplyMode.from(params.get("mode"));
        if (mode == null) {
            mode = EffectApplyMode.PRIMARY;
        }
        boolean pull = Boolean.TRUE.equals(EffectParamUtils.booleanParam(params, "pull"));
        Location casterLocation = ctx.caster().getLocation();
        PveService pveService = ctx.magicService().pveService();
        for (LivingEntity target : EffectTargetResolver.resolveTargets(ctx.plan(), mode)) {
            if (pull) {
                if (pveService.isPullImmune(target)) {
                    ctx.magicService().diagnosticsService().recordPveImmune("pull");
                    continue;
                }
            } else if (pveService.isKnockbackImmune(target)) {
                ctx.magicService().diagnosticsService().recordPveImmune("knockback");
                continue;
            }
            Location targetLocation = target.getLocation();
            Vector direction = pull
                    ? casterLocation.toVector().subtract(targetLocation.toVector())
                    : targetLocation.toVector().subtract(casterLocation.toVector());
            if (direction.lengthSquared() == 0) {
                continue;
            }
            Vector velocity = direction.normalize().multiply(strength);
            velocity.setY(Math.max(0.1, velocity.getY()));
            target.setVelocity(velocity);
        }
    }
}
