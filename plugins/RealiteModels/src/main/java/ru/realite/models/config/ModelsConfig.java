package ru.realite.models.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.joml.Vector3f;

public final class ModelsConfig {

    private final Map<String, ModelDefinition> models;

    private ModelsConfig(Map<String, ModelDefinition> models) {
        this.models = Map.copyOf(models);
    }

    public static ModelsConfig empty() {
        return new ModelsConfig(Map.of());
    }

    public static ModelsConfig load(YamlConfiguration config, Logger logger) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(logger, "logger");

        ConfigurationSection modelsSection = config.getConfigurationSection("models");
        if (modelsSection == null) {
            return empty();
        }

        Map<String, ModelDefinition> definitions = new HashMap<>();
        for (String modelId : modelsSection.getKeys(false)) {
            ConfigurationSection modelSection = modelsSection.getConfigurationSection(modelId);
            if (modelSection == null) {
                continue;
            }
            ModelDefinition definition = parseDefinition(modelId, modelSection, logger);
            if (definition != null) {
                definitions.put(modelId, definition);
            }
        }

        return new ModelsConfig(definitions);
    }

    public Optional<ModelDefinition> find(String modelId) {
        return Optional.ofNullable(models.get(modelId));
    }

    public Map<String, ModelDefinition> all() {
        return Collections.unmodifiableMap(models);
    }

    public Optional<ModelDefinition> matchingFor(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        for (ModelDefinition definition : models.values()) {
            if (definition.matches(entity)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    private static ModelDefinition parseDefinition(String modelId, ConfigurationSection section, Logger logger) {
        String entityKey = section.getString("entity");
        if (entityKey == null || entityKey.isBlank()) {
            logger.warning("[Models] Missing entity for model: " + modelId);
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(entityKey);
        EntityType entityType = key == null ? null : Registry.ENTITY_TYPE.get(key);
        if (entityType == null) {
            logger.warning("[Models] Unknown entity type: " + entityKey + " for model " + modelId);
            return null;
        }

        String nameContains = section.getString("when.name_contains", "");
        ModelCondition condition = nameContains.isBlank() ? null : new ModelCondition(nameContains);

        HorseAppearance appearance = null;
        String colorRaw = section.getString("apply.horse.color", "");
        String styleRaw = section.getString("apply.horse.style", "");
        Horse.Color color = parseEnum(Horse.Color.class, colorRaw);
        Horse.Style style = parseEnum(Horse.Style.class, styleRaw);
        if (color != null || style != null) {
            appearance = new HorseAppearance(color, style);
        }

        String attachmentType = section.getString("apply.attachment.type", "ITEM_DISPLAY");
        if (!"ITEM_DISPLAY".equalsIgnoreCase(attachmentType)) {
            logger.warning("[Models] Unsupported attachment type " + attachmentType + " for model " + modelId);
            return null;
        }

        String itemKey = section.getString("apply.attachment.item", "minecraft:stick");
        Material material = parseMaterial(itemKey);
        if (material == null) {
            logger.warning("[Models] Unknown material " + itemKey + " for model " + modelId + ", using stick.");
            material = Material.STICK;
        }

        Integer customModelData = null;
        if (section.contains("apply.attachment.custom_model_data")) {
            customModelData = section.getInt("apply.attachment.custom_model_data");
        }

        Vector3f offset = parseVector(section, "apply.attachment.transform.offset", new Vector3f(0f, 0f, 0f));
        Vector3f rotation = parseVector(section, "apply.attachment.transform.rotation", new Vector3f(0f, 0f, 0f));
        Vector3f scale = parseVector(section, "apply.attachment.transform.scale", new Vector3f(1f, 1f, 1f));

        AttachmentSpec attachment = new AttachmentSpec(material, customModelData, offset, rotation, scale);
        return new ModelDefinition(modelId, entityType, condition, appearance, attachment);
    }

    private static Material parseMaterial(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        NamespacedKey namespacedKey = NamespacedKey.fromString(key.toLowerCase(Locale.ROOT));
        if (namespacedKey != null) {
            return Registry.MATERIAL.get(namespacedKey);
        }
        return Material.matchMaterial(key);
    }

    private static Vector3f parseVector(ConfigurationSection section, String path, Vector3f fallback) {
        List<Double> doubles = section.getDoubleList(path);
        if (doubles.size() >= 3) {
            return new Vector3f(doubles.get(0).floatValue(), doubles.get(1).floatValue(), doubles.get(2).floatValue());
        }
        List<String> raw = section.getStringList(path);
        if (raw.size() >= 3) {
            List<Float> values = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                try {
                    values.add(Float.parseFloat(raw.get(i)));
                } catch (NumberFormatException ignored) {
                    return new Vector3f(fallback);
                }
            }
            return new Vector3f(values.get(0), values.get(1), values.get(2));
        }
        return new Vector3f(fallback);
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record ModelCondition(String nameContains) {
        public boolean matches(Entity entity) {
            if (nameContains == null || nameContains.isBlank()) {
                return false;
            }
            String customName = entity.getCustomName();
            if (customName == null) {
                return false;
            }
            return customName.contains(nameContains);
        }
    }

    public record HorseAppearance(Horse.Color color, Horse.Style style) {
    }

    public record AttachmentSpec(Material material, Integer customModelData, Vector3f offset,
                                 Vector3f rotation, Vector3f scale) {
    }

    public record ModelDefinition(String id, EntityType entityType, ModelCondition condition,
                                  HorseAppearance horseAppearance, AttachmentSpec attachment) {
        public boolean matches(Entity entity) {
            if (!matchesEntity(entity.getType())) {
                return false;
            }
            if (condition == null) {
                return false;
            }
            return condition.matches(entity);
        }

        public boolean matchesEntity(EntityType type) {
            return entityType == type;
        }
    }
}
