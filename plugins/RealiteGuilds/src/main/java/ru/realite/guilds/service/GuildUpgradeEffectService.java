package ru.realite.guilds.service;

import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import ru.realite.guilds.model.upgrade.UpgradeDefinition;
import ru.realite.guilds.model.upgrade.UpgradeEffect;
import ru.realite.guilds.model.upgrade.UpgradeEffectType;
import ru.realite.guilds.model.upgrade.ValueSpec;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

public final class GuildUpgradeEffectService {

    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildUpgradeConfigRepository upgradeConfig;

    public GuildUpgradeEffectService(FileConfiguration config,
                                     GuildRepository repository,
                                     GuildUpgradeConfigRepository upgradeConfig) {
        this.config = Objects.requireNonNull(config, "config");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.upgradeConfig = Objects.requireNonNull(upgradeConfig, "upgradeConfig");
    }

    public double resolveMemberSlotsBonus(String guildTag) {
        return resolveEffectValue(guildTag, UpgradeEffectType.MEMBER_SLOTS);
    }

    public double resolvePveDamageMultiplier(String guildTag) {
        double raw = resolveEffectValue(guildTag, UpgradeEffectType.PVE_DAMAGE_MULTIPLIER);
        return raw > 0.0d ? raw : 1.0d;
    }

    public boolean isPveEffectEnabled() {
        return config.getBoolean("effects.pveDamageMultiplier.enabled", true);
    }

    public boolean shouldApplyPveInTerritoryOnly() {
        return config.getBoolean("effects.pveDamageMultiplier.requireTerritory", false);
    }

    public boolean shouldApplyPveInWorldsOnly() {
        return config.getBoolean("effects.pveDamageMultiplier.requireWorld", false);
    }

    public List<String> getPveAllowedWorlds() {
        return config.getStringList("effects.pveDamageMultiplier.allowedWorlds");
    }

    private double resolveEffectValue(String guildTag, UpgradeEffectType effectType) {
        if (guildTag == null || guildTag.isBlank() || effectType == null) {
            return 0.0d;
        }
        UpgradeDefinition definition = findUpgradeDefinition(effectType);
        if (definition == null) {
            return 0.0d;
        }
        int level = repository.getUpgradeLevel(guildTag, definition.id());
        if (level <= 0) {
            return 0.0d;
        }
        UpgradeEffect effect = findEffect(definition.effects(), effectType);
        if (effect == null) {
            return 0.0d;
        }
        return resolveValue(effect.value(), level);
    }

    private UpgradeDefinition findUpgradeDefinition(UpgradeEffectType effectType) {
        for (UpgradeDefinition definition : upgradeConfig.getUpgrades().values()) {
            if (definition == null || !definition.enabled()) {
                continue;
            }
            if (findEffect(definition.effects(), effectType) != null) {
                return definition;
            }
        }
        return null;
    }

    private UpgradeEffect findEffect(List<UpgradeEffect> effects, UpgradeEffectType effectType) {
        if (effects == null || effects.isEmpty()) {
            return null;
        }
        for (UpgradeEffect effect : effects) {
            if (effect != null && effect.type() == effectType) {
                return effect;
            }
        }
        return null;
    }

    private double resolveValue(ValueSpec spec, int level) {
        if (spec instanceof ValueSpec.Linear linear) {
            return linear.base() + linear.perLevel() * level;
        }
        if (spec instanceof ValueSpec.Table table) {
            Double value = table.values().get(level);
            if (value == null) {
                return 0.0d;
            }
            return value;
        }
        return 0.0d;
    }
}
