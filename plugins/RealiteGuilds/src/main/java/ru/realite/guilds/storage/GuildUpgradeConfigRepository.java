package ru.realite.guilds.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.model.upgrade.UpgradeCost;
import ru.realite.guilds.model.upgrade.UpgradeCostType;
import ru.realite.guilds.model.upgrade.UpgradeDefinition;
import ru.realite.guilds.model.upgrade.UpgradeEffect;
import ru.realite.guilds.model.upgrade.UpgradeEffectType;
import ru.realite.guilds.model.upgrade.ValueSpec;
import ru.realite.guilds.model.upgrade.ValueSpecType;

public final class GuildUpgradeConfigRepository {

    private final JavaPlugin plugin;
    private final Map<String, UpgradeDefinition> upgrades = new HashMap<>();
    private UpgradeSettings settings = new UpgradeSettings(true, false);

    public GuildUpgradeConfigRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        upgrades.clear();
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        settings = readSettings(config.getConfigurationSection("settings"))
                .orElse(new UpgradeSettings(true, false));
        ConfigurationSection upgradesSection = config.getConfigurationSection("upgrades");
        if (upgradesSection == null) {
            return;
        }
        for (String key : upgradesSection.getKeys(false)) {
            ConfigurationSection upgradeSection = upgradesSection.getConfigurationSection(key);
            if (upgradeSection == null) {
                continue;
            }
            String id = normalizeId(key);
            Optional<UpgradeDefinition> definition = parseDefinition(id, upgradeSection);
            definition.ifPresent(value -> upgrades.put(id, value));
        }
    }

    public UpgradeSettings getSettings() {
        return settings;
    }

    public Map<String, UpgradeDefinition> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    private Optional<UpgradeSettings> readSettings(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        boolean requirePermission = section.getBoolean("requirePermission", true);
        boolean allowNegative = section.getBoolean("allowNegative", false);
        return Optional.of(new UpgradeSettings(requirePermission, allowNegative));
    }

    private Optional<UpgradeDefinition> parseDefinition(String id, ConfigurationSection section) {
        if (!section.isBoolean("enabled")) {
            plugin.getLogger().severe("Upgrade '" + id + "' has invalid 'enabled' value, must be true/false.");
            return Optional.of(disabledDefinition(id, section, "Invalid enabled flag"));
        }
        boolean enabled = section.getBoolean("enabled");
        String name = section.getString("name", id);
        String description = section.getString("description", "");
        int maxLevel = section.getInt("maxLevel", 0);
        if (maxLevel < 1) {
            plugin.getLogger().severe("Upgrade '" + id + "' has invalid maxLevel: " + maxLevel);
            return Optional.of(disabledDefinition(id, section, "Invalid maxLevel"));
        }
        ConfigurationSection purchaseSection = section.getConfigurationSection("purchase");
        if (purchaseSection == null) {
            plugin.getLogger().severe("Upgrade '" + id + "' missing purchase section.");
            return Optional.of(disabledDefinition(id, section, "Missing purchase section"));
        }
        ConfigurationSection costSection = purchaseSection.getConfigurationSection("cost");
        if (costSection == null) {
            plugin.getLogger().severe("Upgrade '" + id + "' missing purchase.cost section.");
            return Optional.of(disabledDefinition(id, section, "Missing cost section"));
        }
        UpgradeCost cost = parseCost(id, costSection, maxLevel).orElse(null);
        if (cost == null) {
            return Optional.of(disabledDefinition(id, section, "Invalid cost"));
        }
        Map<String, Integer> requirements = readRequirements(purchaseSection.getConfigurationSection("requirements"));
        List<UpgradeEffect> effects = readEffects(id, section.getMapList("effects"), maxLevel);
        if (effects == null) {
            return Optional.of(disabledDefinition(id, section, "Invalid effects"));
        }
        return Optional.of(new UpgradeDefinition(id, enabled, name, description, maxLevel, cost, requirements, effects));
    }

    private Optional<UpgradeCost> parseCost(String id, ConfigurationSection costSection, int maxLevel) {
        String rawType = costSection.getString("type", "");
        Optional<UpgradeCostType> type = parseCostType(id, rawType);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        if (type.get() == UpgradeCostType.FORMULA) {
            String formula = costSection.getString("formula", "").trim();
            if (formula.isEmpty()) {
                plugin.getLogger().severe("Upgrade '" + id + "' has empty formula for cost.");
                return Optional.empty();
            }
            return Optional.of(new UpgradeCost.Formula(formula));
        }
        Map<Integer, Double> values = parseLevelTable(id, "purchase.cost", costSection.get("table"), maxLevel);
        if (values == null) {
            return Optional.empty();
        }
        return Optional.of(new UpgradeCost.Table(values));
    }

    private Optional<UpgradeCostType> parseCostType(String id, String rawType) {
        if (rawType == null || rawType.isBlank()) {
            plugin.getLogger().severe("Upgrade '" + id + "' is missing cost.type.");
            return Optional.empty();
        }
        try {
            return Optional.of(UpgradeCostType.valueOf(rawType.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().severe("Upgrade '" + id + "' has unknown cost.type: " + rawType);
            return Optional.empty();
        }
    }

    private List<UpgradeEffect> readEffects(String id, List<Map<?, ?>> rawEffects, int maxLevel) {
        if (rawEffects == null || rawEffects.isEmpty()) {
            return Collections.emptyList();
        }
        List<UpgradeEffect> effects = new ArrayList<>();
        for (Map<?, ?> rawEffect : rawEffects) {
            if (rawEffect == null) {
                continue;
            }
            Object rawType = rawEffect.get("type");
            if (!(rawType instanceof String)) {
                plugin.getLogger().severe("Upgrade '" + id + "' has effect without type.");
                return null;
            }
            UpgradeEffectType effectType;
            try {
                effectType = UpgradeEffectType.valueOf(((String) rawType).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().severe("Upgrade '" + id + "' has unknown effect type: " + rawType);
                return null;
            }
            Object rawValue = rawEffect.get("value");
            if (!(rawValue instanceof Map<?, ?> valueMap)) {
                plugin.getLogger().severe("Upgrade '" + id + "' effect '" + rawType + "' missing value section.");
                return null;
            }
            ValueSpec valueSpec = parseValueSpec(id, effectType, valueMap, maxLevel);
            if (valueSpec == null) {
                return null;
            }
            effects.add(new UpgradeEffect(effectType, valueSpec));
        }
        return effects;
    }

    private ValueSpec parseValueSpec(String id, UpgradeEffectType effectType, Map<?, ?> rawValue, int maxLevel) {
        Object rawType = rawValue.get("type");
        if (!(rawType instanceof String)) {
            plugin.getLogger().severe("Upgrade '" + id + "' effect '" + effectType + "' missing value.type.");
            return null;
        }
        ValueSpecType valueType;
        try {
            valueType = ValueSpecType.valueOf(((String) rawType).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().severe("Upgrade '" + id + "' effect '" + effectType + "' has unknown value.type: " + rawType);
            return null;
        }
        if (valueType == ValueSpecType.LINEAR) {
            Object baseRaw = rawValue.get("base");
            Object perLevelRaw = rawValue.get("perLevel");
            if (!(baseRaw instanceof Number) || !(perLevelRaw instanceof Number)) {
                plugin.getLogger().severe("Upgrade '" + id + "' effect '" + effectType + "' has invalid linear values.");
                return null;
            }
            return new ValueSpec.Linear(((Number) baseRaw).doubleValue(), ((Number) perLevelRaw).doubleValue());
        }
        Map<Integer, Double> values = parseLevelTable(id,
                "effect." + effectType + ".value",
                rawValue.get("table"),
                maxLevel);
        if (values == null) {
            return null;
        }
        return new ValueSpec.Table(values);
    }

    private Map<String, Integer> readRequirements(ConfigurationSection requirementsSection) {
        if (requirementsSection == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> requirements = new LinkedHashMap<>();
        for (String key : requirementsSection.getKeys(false)) {
            requirements.put(key, requirementsSection.getInt(key));
        }
        return requirements;
    }

    private UpgradeDefinition disabledDefinition(String id, ConfigurationSection section, String reason) {
        String name = section.getString("name", id);
        String description = section.getString("description", "");
        plugin.getLogger().warning("Upgrade '" + id + "' disabled due to config error: " + reason);
        return new UpgradeDefinition(id, false, name, description, 1,
                new UpgradeCost.Table(Map.of(1, 0.0d)),
                Collections.emptyMap(),
                Collections.emptyList());
    }

    private Map<Integer, Double> parseLevelTable(String id, String context, Object rawTable, int maxLevel) {
        if (!(rawTable instanceof Map<?, ?> table)) {
            plugin.getLogger().severe("Upgrade '" + id + "' missing " + context + ".table values.");
            return null;
        }
        Map<Integer, Double> values = new LinkedHashMap<>();
        boolean valid = true;
        for (int level = 1; level <= maxLevel; level++) {
            Object rawValue = table.get(level);
            if (rawValue == null) {
                rawValue = table.get(String.valueOf(level));
            }
            if (!(rawValue instanceof Number)) {
                plugin.getLogger().severe("Upgrade '" + id + "' missing " + context + " value for level " + level);
                valid = false;
                continue;
            }
            values.put(level, ((Number) rawValue).doubleValue());
        }
        if (!valid) {
            return null;
        }
        return values;
    }

    private String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public record UpgradeSettings(boolean requirePermission, boolean allowNegative) {
    }
}
