package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

public final class ParticlesEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "particles";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object particleRaw = params.get("particle");
        String particleName = EffectParamUtils.stringParam(params, "particle");
        if (particleRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "particle"));
        }
        if (particleName == null || parseParticle(particleName) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "particle", "value", String.valueOf(particleRaw)));
        }
        Object countRaw = params.get("count");
        Integer count = EffectParamUtils.intParam(params, "count");
        if (countRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "count"));
        }
        if (count == null || count <= 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "count", "value", String.valueOf(countRaw)));
        }
        Object spreadRaw = params.get("spread");
        Double spread = EffectParamUtils.doubleParam(params, "spread");
        if (spreadRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "spread"));
        }
        if (spread == null || spread < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "spread", "value", String.valueOf(spreadRaw)));
        }
        Object targetRaw = params.get("target");
        EffectTargetType targetType = EffectTargetType.from(targetRaw);
        if (targetRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "target"));
        }
        if (targetType != EffectTargetType.LOCATION
                && targetType != EffectTargetType.ORIGIN
                && targetType != EffectTargetType.IMPACT
                && targetType != EffectTargetType.PRIMARY) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "target", "value", String.valueOf(targetRaw)));
        }
        return EffectValidationResult.ok();
    }

    @Override
    public void execute(EffectContext ctx, SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        String particleName = EffectParamUtils.stringParam(params, "particle");
        Particle particle = particleName == null ? null : parseParticle(particleName);
        Integer count = EffectParamUtils.intParam(params, "count");
        Double spread = EffectParamUtils.doubleParam(params, "spread");
        if (particle == null || count == null || spread == null || count <= 0 || spread < 0) {
            return;
        }
        EffectTargetType targetType = EffectTargetType.from(params.get("target"));
        Location location = EffectTargetResolver.resolveLocation(ctx.plan(), targetType, ctx.caster());
        if (location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(particle, location, count, spread, spread, spread, (Object) null);
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
