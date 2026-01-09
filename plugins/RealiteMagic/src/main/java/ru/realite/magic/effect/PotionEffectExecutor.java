package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public final class PotionEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "potion";
    }

    private static PotionEffectType resolvePotionType(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        if (key.isBlank()) {
            return null;
        }
        return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object effectRaw = params.get("effect");
        String effectName = EffectParamUtils.stringParam(params, "effect");
        if (effectRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "effect"));
        }
        if (effectName == null || resolvePotionType(effectName) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "effect", "value", String.valueOf(effectRaw)));
        }
        Object durationRaw = params.get("durationTicks");
        Integer duration = EffectParamUtils.intParam(params, "durationTicks");
        if (durationRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "durationTicks"));
        }
        if (duration == null || duration <= 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "durationTicks", "value", String.valueOf(durationRaw)));
        }
        Object amplifierRaw = params.get("amplifier");
        Integer amplifier = EffectParamUtils.intParam(params, "amplifier");
        if (amplifierRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "amplifier"));
        }
        if (amplifier == null || amplifier < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "amplifier", "value", String.valueOf(amplifierRaw)));
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
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        String effectName = EffectParamUtils.stringParam(params, "effect");
        PotionEffectType type = resolvePotionType(effectName);
        Integer duration = EffectParamUtils.intParam(params, "durationTicks");
        Integer amplifier = EffectParamUtils.intParam(params, "amplifier");
        if (type == null || duration == null || amplifier == null || duration <= 0 || amplifier < 0) {
            return;
        }
        EffectApplyMode mode = EffectApplyMode.from(params.get("mode"));
        if (mode == null) {
            mode = EffectApplyMode.PRIMARY;
        }
        for (LivingEntity target : EffectTargetResolver.resolveTargets(ctx.plan(), mode)) {
            target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        }
    }
}
