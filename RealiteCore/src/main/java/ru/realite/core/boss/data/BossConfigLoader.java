package ru.realite.core.boss.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
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

        String id = readRequiredString(config, "id", fileErrors);
        String name = readOptionalString(config, "name");
        String entityName = readRequiredString(config, "entity", fileErrors);
        EntityType entityType = parseEntityType(entityName, fileErrors);
        double maxHp = config.getDouble("max_hp", -1.0);
        if (maxHp <= 0.0) {
            fileErrors.add("max_hp must be a positive number");
        }

        List<BossPhaseDefinition> phases = readPhases(config, fileErrors);
        List<String> abilityIds = readAbilities(config, fileErrors);

        if (!fileErrors.isEmpty()) {
            errors.add(formatErrors(file, fileErrors));
            return;
        }

        BossDefinition definition = new BossDefinition(
                id,
                name,
                entityType,
                maxHp,
                phases,
                abilityIds
        );
        try {
            registry.register(definition);
        } catch (IllegalArgumentException e) {
            errors.add(formatErrors(file, List.of(e.getMessage())));
        }
    }

    private String readRequiredString(YamlConfiguration config, String key, List<String> errors) {
        String value = readOptionalString(config, key);
        if (value == null) {
            errors.add(key + " is required");
        }
        return value;
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
            Double threshold = readDouble(values, "enter_at");
            if (threshold == null) {
                errors.add("phases[" + index + "].enter_at is required");
                continue;
            }
            phases.add(new BossPhaseDefinition(phaseId, threshold));
        }
        return phases;
    }

    private List<String> readAbilities(YamlConfiguration config, List<String> errors) {
        List<String> abilityIds = new ArrayList<>();
        for (String abilityId : config.getStringList("abilities")) {
            if (abilityId == null || abilityId.isBlank()) {
                errors.add("abilities contains a blank id");
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

    private String formatErrors(File file, List<String> fileErrors) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(file.getPath()).append(':');
        for (String error : fileErrors) {
            builder.append("\n  - ").append(error);
        }
        return builder.toString();
    }
}
