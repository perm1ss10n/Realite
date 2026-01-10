package ru.realite.magic.pve;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellDefinition;

public final class PveService {

    private final JavaPlugin plugin;
    private boolean enabled;
    private EntityMagicProfileResolver resolver;
    private final MagicHitLimiter hitLimiter = new MagicHitLimiter();

    public PveService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload(plugin.getConfig());
    }

    public boolean enabled() {
        return enabled;
    }

    public void reload(FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        this.enabled = config.getBoolean("pve.enabled", false);
        this.resolver = new EntityMagicProfileResolver(plugin, config, enabled);
    }

    public double damageTakenMultiplier(@Nullable SpellDefinition spell, LivingEntity target) {
        if (!enabled || target == null) {
            return 1.0;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        double multiplier = profile.damageTakenMultiplier();
        MagicSchool school = spell == null ? null : spell.school();
        return multiplier * profile.schoolMultiplier(school);
    }

    public boolean isEffectImmune(SpellEffectDefinition effect, @Nullable SpellDefinition spell, LivingEntity target) {
        if (!enabled || effect == null || target == null) {
            return false;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        return profile.isEffectImmune(effect.type());
    }

    public boolean isPotionImmune(@Nullable org.bukkit.potion.PotionEffectType type, LivingEntity target) {
        if (!enabled || type == null || target == null) {
            return false;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        return profile.isPotionImmune(type);
    }

    public boolean isKnockbackImmune(LivingEntity target) {
        if (!enabled || target == null) {
            return false;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        return profile.immuneKnockback() || profile.isEffectImmune("knockback");
    }

    public boolean isPullImmune(LivingEntity target) {
        if (!enabled || target == null) {
            return false;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        return profile.immunePull() || profile.isEffectImmune("pull");
    }

    public boolean isTeleportImmune(LivingEntity target) {
        if (!enabled || target == null) {
            return false;
        }
        EntityMagicProfile profile = resolver.resolve(target);
        return profile.immuneTeleport() || profile.isEffectImmune("teleport");
    }

    public boolean allowHit(UUID caster, UUID target, LivingEntity targetEntity) {
        if (!enabled || targetEntity == null) {
            return true;
        }
        EntityMagicProfile profile = resolver.resolve(targetEntity);
        return hitLimiter.allowHit(caster, target, profile);
    }
}
