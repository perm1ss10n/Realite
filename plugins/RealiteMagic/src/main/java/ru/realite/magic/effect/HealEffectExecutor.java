package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public final class HealEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "heal";
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
        if (amount == null || amount <= 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "amount", "value", String.valueOf(amountRaw)));
        }
        Object modeRaw = params.get("mode");
        if (modeRaw != null && EffectApplyMode.from(modeRaw) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "mode", "value", String.valueOf(modeRaw)));
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        if (ctx == null || def == null) {
            return;
        }
        Map<String, Object> params = def.params();
        Double amount = EffectParamUtils.doubleParam(params, "amount");
        if (amount == null || amount <= 0) {
            return;
        }
        EffectApplyMode mode = EffectApplyMode.from(params.get("mode"));
        if (mode == null) {
            mode = EffectApplyMode.PRIMARY;
        }
        for (LivingEntity target : EffectTargetResolver.resolveTargets(ctx.plan(), mode)) {
            if (target == null) {
                continue;
            }
            double maxHealth = maxHealth(target);
            double next = Math.min(maxHealth, target.getHealth() + amount);
            target.setHealth(next);
        }
    }

    private double maxHealth(LivingEntity target) {
        AttributeInstance attribute = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 0.0;
    }
}
