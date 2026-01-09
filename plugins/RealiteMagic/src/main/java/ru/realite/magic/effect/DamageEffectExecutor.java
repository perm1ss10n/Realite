package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

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
        EffectTargetType targetType = EffectTargetType.from(targetRaw);
        if (targetRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "target"));
        }
        if (targetType != EffectTargetType.ENTITY) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "target", "value", String.valueOf(targetRaw)));
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
        LivingEntity target = EffectTargetResolver.resolveEntity(ctx.target());
        if (target == null) {
            return;
        }
        target.damage(amount, ctx.caster());
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
