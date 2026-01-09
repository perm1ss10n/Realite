package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import ru.realite.magic.pve.PveService;

public final class DamageEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "damage";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object amountRaw = params.get("amount");
        Double amount = EffectParamUtils.doubleParam(params, "amount");
        if (amountRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "amount"));
        }
        if (amount == null || amount < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "amount", "value", String.valueOf(amountRaw)));
        }
        Object targetRaw = params.get("target");
        if (targetRaw != null) {
            EffectTargetType targetType = EffectTargetType.from(targetRaw);
            if (targetType != EffectTargetType.ENTITY) {
                return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                        Map.of("type", def.type(), "param", "target", "value", String.valueOf(targetRaw)));
            }
        }
        Object modeRaw = params.get("mode");
        if (modeRaw != null && EffectApplyMode.from(modeRaw) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "mode", "value", String.valueOf(modeRaw)));
        }
        Object causeRaw = params.get("cause");
        if (causeRaw != null) {
            if (parseDamageCause(String.valueOf(causeRaw)) == null) {
                return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                        Map.of("type", def.type(), "param", "cause", "value", String.valueOf(causeRaw)));
            }
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Double amount = EffectParamUtils.doubleParam(params, "amount");
        if (amount == null || amount <= 0) {
            return;
        }
        double multiplier = ctx.modifiers().damageMultiplier();
        double baseDamage = multiplier == 1.0 ? amount : amount * multiplier;
        EffectApplyMode mode = EffectApplyMode.from(params.get("mode"));
        if (mode == null) {
            mode = EffectApplyMode.PRIMARY;
        }
        PveService pveService = ctx.magicService().pveService();
        for (LivingEntity target : EffectTargetResolver.resolveTargets(ctx.plan(), mode)) {
            if (pveService.isEffectImmune(def, ctx.spell(), target)) {
                continue;
            }
            if (!pveService.allowHit(ctx.caster().getUniqueId(), target.getUniqueId(), target)) {
                continue;
            }
            double targetMultiplier = pveService.damageTakenMultiplier(ctx.spell(), target);
            double finalDamage = baseDamage * targetMultiplier;
            if (finalDamage <= 0) {
                continue;
            }
            target.damage(finalDamage, ctx.caster());
        }
    }

    private DamageCause parseDamageCause(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return DamageCause.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
