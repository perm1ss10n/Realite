package ru.realite.magic.effect;

import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;

public final class SoundEffectExecutor implements SpellEffectExecutor {

    @Override
    public String type() {
        return "sound";
    }

    @Override
    public EffectValidationResult validate(SpellEffectDefinition def) {
        Map<String, Object> params = def.params();
        Object soundRaw = params.get("sound");
        String soundName = EffectParamUtils.stringParam(params, "sound");
        if (soundRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "sound"));
        }
        if (soundName == null || parseSound(soundName) == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "sound", "value", String.valueOf(soundRaw)));
        }
        Object volumeRaw = params.get("volume");
        Double volume = EffectParamUtils.doubleParam(params, "volume");
        if (volumeRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "volume"));
        }
        if (volume == null || volume < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "volume", "value", String.valueOf(volumeRaw)));
        }
        Object pitchRaw = params.get("pitch");
        Double pitch = EffectParamUtils.doubleParam(params, "pitch");
        if (pitchRaw == null) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_missing_param",
                    Map.of("type", def.type(), "param", "pitch"));
        }
        if (pitch == null || pitch < 0) {
            return EffectValidationResult.fail("magic.cmd.spells.errors.effect_invalid_param",
                    Map.of("type", def.type(), "param", "pitch", "value", String.valueOf(pitchRaw)));
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
        if (ctx == null || def == null) {
            return;
        }
        Map<String, Object> params = def.params();
        String soundName = EffectParamUtils.stringParam(params, "sound");
        Sound sound = soundName == null ? null : parseSound(soundName);
        Double volume = EffectParamUtils.doubleParam(params, "volume");
        Double pitch = EffectParamUtils.doubleParam(params, "pitch");
        if (sound == null || volume == null || pitch == null || volume < 0 || pitch < 0) {
            return;
        }
        EffectTargetType targetType = EffectTargetType.from(params.get("target"));
        if (targetType == null) {
            return;
        }
        Location location = EffectTargetResolver.resolveLocation(ctx.plan(), targetType, ctx.caster());
        if (location == null) {
            return;
        }
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, sound, volume.floatValue(), pitch.floatValue());
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
