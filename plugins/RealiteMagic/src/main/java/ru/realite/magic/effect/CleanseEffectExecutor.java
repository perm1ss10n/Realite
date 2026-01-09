package ru.realite.magic.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class CleanseEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "cleanse";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object modeRaw = params.get("mode");
        if (modeRaw != null && EffectApplyMode.from(modeRaw) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "mode", "value", String.valueOf(modeRaw)));
        }
        boolean hasRemoveNegative = params.get("removeNegative") != null;
        if (hasRemoveNegative && EffectParamUtils.booleanParam(params, "removeNegative") == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "removeNegative", "value", String.valueOf(params.get("removeNegative"))));
        }
        Object removeRaw = params.get("remove");
        if (removeRaw != null) {
            List<String> removeList = parseRemoveList(removeRaw);
            if (removeList == null) {
                return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                        Map.of("type", def.type(), "param", "remove", "value", String.valueOf(removeRaw)));
            }
            for (String name : removeList) {
                if (PotionEffectType.getByName(name.toUpperCase(Locale.ROOT)) == null) {
                    return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                            Map.of("type", def.type(), "param", "remove", "value", name));
                }
            }
        }
        if (!hasRemoveNegative && removeRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "removeNegative"));
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        EffectApplyMode mode = EffectApplyMode.from(params.get("mode"));
        if (mode == null) {
            mode = EffectApplyMode.PRIMARY;
        }
        List<String> removeList = parseRemoveList(params.get("remove"));
        boolean removeNegative = Boolean.TRUE.equals(EffectParamUtils.booleanParam(params, "removeNegative"));
        for (LivingEntity target : EffectTargetResolver.resolveTargets(ctx.plan(), mode)) {
            if (removeList != null && !removeList.isEmpty()) {
                for (String name : removeList) {
                    PotionEffectType type = PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
                    if (type != null) {
                        target.removePotionEffect(type);
                    }
                }
                continue;
            }
            if (removeNegative) {
                for (PotionEffect effect : target.getActivePotionEffects()) {
                    PotionEffectType type = effect.getType();
                    if (!type.isBeneficial()) {
                        target.removePotionEffect(type);
                    }
                }
            }
        }
    }

    private List<String> parseRemoveList(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object entry : list) {
                if (entry == null) {
                    return null;
                }
                String value = String.valueOf(entry).trim();
                if (value.isBlank()) {
                    return null;
                }
                values.add(value);
            }
            return values;
        }
        if (raw instanceof String str) {
            String value = str.trim();
            if (value.isBlank()) {
                return null;
            }
            return List.of(value);
        }
        return null;
    }
}
