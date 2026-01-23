package ru.realite.core.boss.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.boss.core.BossAbilityDefaults;
import ru.realite.core.boss.core.BossAbilityRegistry;
import ru.realite.core.boss.core.BossRegistry;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BossConfigLoader {
    private static final String DEFAULT_BOSS_RESOURCE = "bosses/boss_first.yml";

    private final JavaPlugin plugin;
    private final BossRegistry registry;
    private final BossAbilityRegistry abilityRegistry;

    public BossConfigLoader(JavaPlugin plugin, BossRegistry registry, BossAbilityRegistry abilityRegistry) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin is null");
        }
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (abilityRegistry == null) {
            throw new IllegalArgumentException("abilityRegistry is null");
        }
        this.plugin = plugin;
        this.registry = registry;
        this.abilityRegistry = abilityRegistry;
    }

    public void loadAll() {
        BossAbilityDefaults.registerDefaults(abilityRegistry);
        registry.clear();
        Path bossesDir = plugin.getDataFolder().toPath().resolve("bosses");
        ensureDirectory(bossesDir);
        saveDefaultBossConfig(bossesDir);

        List<String> errors = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bossesDir, "*.yml")) {
            for (Path file : stream) {
                loadSingle(file.toFile(), errors);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan bosses directory: " + bossesDir, e);
        }

        if (!errors.isEmpty()) {
            String message = "Boss config errors:\n" + String.join("\n", errors);
            throw new IllegalStateException(message);
        }
    }

    private void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create bosses directory: " + path, e);
        }
    }

    private void saveDefaultBossConfig(Path bossesDir) {
        Path defaultConfig = bossesDir.resolve("boss_first.yml");
        if (Files.exists(defaultConfig)) {
            return;
        }
        if (plugin.getResource(DEFAULT_BOSS_RESOURCE) == null) {
            return;
        }
        plugin.saveResource(DEFAULT_BOSS_RESOURCE, false);
    }

    private void loadSingle(File file, List<String> errors) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> fileErrors = new ArrayList<>();

        String id = readOptionalString(config, "bossId");
        if (id == null) {
            id = readOptionalString(config, "id");
        }
        if (id == null) {
            fileErrors.add("bossId is required");
        }
        String name = readOptionalString(config, "name");
        int tier = config.getInt("tier", 1);
        if (tier <= 0) {
            fileErrors.add("tier must be a positive number");
        }

        String entityName = readOptionalString(config, "entity.type");
        if (entityName == null) {
            entityName = readOptionalString(config, "entity");
        }
        if (entityName == null) {
            fileErrors.add("entity.type is required");
        }
        EntityType entityType = parseEntityType(entityName, fileErrors);

        double maxHp = config.getDouble("stats.maxHp", -1.0);
        if (maxHp <= 0.0) {
            maxHp = config.getDouble("max_hp", -1.0);
        }
        if (maxHp <= 0.0) {
            fileErrors.add("stats.maxHp must be a positive number");
        }
        double baseDamage = config.getDouble("stats.baseDamage", 0.0);
        double movementSpeed = config.getDouble("stats.movementSpeed", 0.0);
        if (baseDamage < 0.0) {
            fileErrors.add("stats.baseDamage must be >= 0");
        }
        if (movementSpeed < 0.0) {
            fileErrors.add("stats.movementSpeed must be >= 0");
        }
        BossStatsDefinition stats = (maxHp > 0.0 && baseDamage >= 0.0 && movementSpeed >= 0.0)
                ? new BossStatsDefinition(maxHp, baseDamage, movementSpeed)
                : null;

        String modelId = readOptionalString(config, "entity.modelId");
        if (modelId == null) {
            modelId = readOptionalString(config, "modelId");
        }

        BossEquipmentDefinition equipment = readEquipment(config);
        List<BossPhaseDefinition> phases = readPhases(config, fileErrors);
        List<String> abilityIds = readAbilities(config, fileErrors);
        BossLootDefinition loot = readLoot(config, fileErrors);
        int maxActiveInstances = config.getInt("maxActiveInstances", 0);
        if (maxActiveInstances < 0) {
            fileErrors.add("maxActiveInstances must be >= 0");
        }

        if (!fileErrors.isEmpty()) {
            errors.add(formatErrors(file, fileErrors));
            return;
        }

        BossDefinition definition = new BossDefinition(
                id,
                name,
                tier,
                entityType,
                stats,
                modelId,
                equipment,
                phases,
                abilityIds,
                loot,
                maxActiveInstances
        );
        try {
            registry.register(definition);
        } catch (IllegalArgumentException e) {
            errors.add(formatErrors(file, List.of(e.getMessage())));
        }
    }

    private String readOptionalString(YamlConfiguration config, String key) {
        String value = config.getString(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private EntityType parseEntityType(String entityName, List<String> errors) {
        if (entityName == null) {
            return null;
        }
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityName.toUpperCase());
        } catch (IllegalArgumentException e) {
            errors.add("entity has unknown type: " + entityName);
            return null;
        }
        if (!entityType.isAlive()) {
            errors.add("entity type is not a living entity: " + entityName);
            return null;
        }
        return entityType;
    }

    private List<BossPhaseDefinition> readPhases(YamlConfiguration config, List<String> errors) {
        List<BossPhaseDefinition> phases = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList("phases");
        for (int index = 0; index < raw.size(); index++) {
            Map<?, ?> data = raw.get(index);
            if (data == null) {
                errors.add("phases[" + index + "] is null");
                continue;
            }
            Map<String, Object> values = new HashMap<>();
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                if (entry.getKey() != null) {
                    values.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            String phaseId = readString(values, "id");
            if (phaseId == null) {
                errors.add("phases[" + index + "].id is required");
                continue;
            }
            Double threshold = readDouble(values, "enterAtHpPercent");
            if (threshold == null) {
                threshold = readDouble(values, "enter_at");
            }
            if (threshold == null) {
                errors.add("phases[" + index + "].enterAtHpPercent is required");
                continue;
            }
            if (threshold > 1.0) {
                threshold = threshold / 100.0;
            }
            phases.add(new BossPhaseDefinition(phaseId, threshold));
        }
        return phases;
    }

    private List<String> readAbilities(YamlConfiguration config, List<String> errors) {
        List<String> abilityIds = new ArrayList<>();
        List<?> raw = config.getList("abilities");
        if (raw == null) {
            return abilityIds;
        }
        for (int index = 0; index < raw.size(); index++) {
            Object entry = raw.get(index);
            String abilityId = null;
            if (entry instanceof String text) {
                abilityId = text;
            } else if (entry instanceof Map<?, ?> data) {
                Map<String, Object> values = new HashMap<>();
                for (Map.Entry<?, ?> mapEntry : data.entrySet()) {
                    if (mapEntry.getKey() != null) {
                        values.put(String.valueOf(mapEntry.getKey()), mapEntry.getValue());
                    }
                }
                abilityId = readString(values, "id");
            }

            if (abilityId == null || abilityId.isBlank()) {
                errors.add("abilities[" + index + "] has blank id");
                continue;
            }
            if (!abilityRegistry.isRegistered(abilityId)) {
                errors.add("abilities contains unknown id: " + abilityId);
                continue;
            }
            abilityIds.add(abilityId);
        }
        return abilityIds;
    }

    private BossEquipmentDefinition readEquipment(YamlConfiguration config) {
        String mainHand = readOptionalString(config, "entity.equipment.mainHand");
        String offHand = readOptionalString(config, "entity.equipment.offHand");
        String helmet = readOptionalString(config, "entity.equipment.helmet");
        String chestplate = readOptionalString(config, "entity.equipment.chestplate");
        String leggings = readOptionalString(config, "entity.equipment.leggings");
        String boots = readOptionalString(config, "entity.equipment.boots");

        if (mainHand == null && offHand == null && helmet == null && chestplate == null && leggings == null && boots == null) {
            mainHand = readOptionalString(config, "equipment.mainHand");
            offHand = readOptionalString(config, "equipment.offHand");
            helmet = readOptionalString(config, "equipment.helmet");
            chestplate = readOptionalString(config, "equipment.chestplate");
            leggings = readOptionalString(config, "equipment.leggings");
            boots = readOptionalString(config, "equipment.boots");
        }

        return new BossEquipmentDefinition(mainHand, offHand, helmet, chestplate, leggings, boots);
    }

    private BossLootDefinition readLoot(YamlConfiguration config, List<String> errors) {
        List<BossGuaranteedDrop> guaranteed = new ArrayList<>();
        List<Map<?, ?>> guaranteedRaw = config.getMapList("loot.guaranteed");
        for (int index = 0; index < guaranteedRaw.size(); index++) {
            Map<?, ?> data = guaranteedRaw.get(index);
            if (data == null) {
                errors.add("loot.guaranteed[" + index + "] is null");
                continue;
            }
            Map<String, Object> values = normalizeMap(data);
            String itemId = readString(values, "itemId");
            if (itemId == null) {
                errors.add("loot.guaranteed[" + index + "].itemId is required");
                continue;
            }
            Integer amount = readInt(values, "amount");
            if (amount == null) {
                amount = 1;
            }
            if (amount <= 0) {
                errors.add("loot.guaranteed[" + index + "].amount must be positive");
                continue;
            }
            guaranteed.add(new BossGuaranteedDrop(itemId, amount));
        }

        List<BossLootEntry> table = new ArrayList<>();
        List<Map<?, ?>> tableRaw = config.getMapList("loot.table");
        for (int index = 0; index < tableRaw.size(); index++) {
            Map<?, ?> data = tableRaw.get(index);
            if (data == null) {
                errors.add("loot.table[" + index + "] is null");
                continue;
            }
            Map<String, Object> values = normalizeMap(data);
            String itemId = readString(values, "itemId");
            if (itemId == null) {
                errors.add("loot.table[" + index + "].itemId is required");
                continue;
            }
            Integer weight = readInt(values, "weight");
            Integer min = readInt(values, "min");
            Integer max = readInt(values, "max");
            if (weight == null || min == null || max == null) {
                errors.add("loot.table[" + index + "] must include weight, min, max");
                continue;
            }
            if (weight <= 0 || min <= 0 || max <= 0 || max < min) {
                errors.add("loot.table[" + index + "] has invalid weight/min/max");
                continue;
            }
            table.add(new BossLootEntry(itemId, min, max, weight));
        }

        int rolls = config.getInt("loot.rolls", 0);
        if (rolls < 0) {
            errors.add("loot.rolls must be >= 0");
            rolls = 0;
        }

        return new BossLootDefinition(guaranteed, table, rolls);
    }

    private Map<String, Object> normalizeMap(Map<?, ?> data) {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<?, ?> entry : data.entrySet()) {
            if (entry.getKey() != null) {
                values.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return values;
    }

    private String readString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Double readDouble(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer readInt(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatErrors(File file, List<String> fileErrors) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(file.getPath()).append(':');
        for (String error : fileErrors) {
            builder.append("\n  - ").append(error);
        }
        return builder.toString();
    }
}
