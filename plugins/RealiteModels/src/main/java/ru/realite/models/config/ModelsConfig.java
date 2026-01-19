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
import org.bukkit.inventory.EquipmentSlot;
import org.joml.Vector3f;

public final class ModelsConfig {

    private static final int ENTITY_CMD_MIN = 12000;
    private static final int ENTITY_CMD_MAX = 12999;
    private static final int ARMOR_CMD_MIN = 40000;
    private static final int ARMOR_CMD_MAX = 49999;

    private final Map<String, EntityModelDefinition> entityModels;
    private final Map<String, ArmorModelDefinition> armorModels;

    private ModelsConfig(Map<String, EntityModelDefinition> entityModels,
                         Map<String, ArmorModelDefinition> armorModels) {
        this.entityModels = Map.copyOf(entityModels);
        this.armorModels = Map.copyOf(armorModels);
    }

    public static ModelsConfig empty() {
        return new ModelsConfig(Map.of(), Map.of());
    }

    public static ModelsConfig load(YamlConfiguration config, Logger logger) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(logger, "logger");

        Map<String, EntityModelDefinition> entityDefinitions = new HashMap<>();
        ConfigurationSection entitySection = config.getConfigurationSection("entity_models");
        if (entitySection == null) {
            entitySection = config.getConfigurationSection("models");
        }
        if (entitySection != null) {
            for (String modelId : entitySection.getKeys(false)) {
                ConfigurationSection modelSection = entitySection.getConfigurationSection(modelId);
                if (modelSection == null) {
                    continue;
                }
                EntityModelDefinition definition = parseEntityDefinition(modelId, modelSection, logger);
                if (definition != null) {
                    entityDefinitions.put(modelId, definition);
                }
            }
        }

        Map<String, ArmorModelDefinition> armorDefinitions = new HashMap<>();
        ConfigurationSection armorSection = config.getConfigurationSection("armor_models");
        if (armorSection != null) {
            for (String itemId : armorSection.getKeys(false)) {
                ConfigurationSection modelSection = armorSection.getConfigurationSection(itemId);
                if (modelSection == null) {
                    continue;
                }
                ArmorModelDefinition definition = parseArmorDefinition(itemId, modelSection, logger);
                if (definition != null) {
                    armorDefinitions.put(itemId, definition);
                }
            }
        }

        return new ModelsConfig(entityDefinitions, armorDefinitions);
    }

    public Optional<EntityModelDefinition> findEntityModel(String modelId) {
        return Optional.ofNullable(entityModels.get(modelId));
    }

    public Optional<ArmorModelDefinition> findArmorModel(String itemId) {
        return Optional.ofNullable(armorModels.get(itemId));
    }

    public Map<String, EntityModelDefinition> allEntityModels() {
        return Collections.unmodifiableMap(entityModels);
    }

    public Map<String, ArmorModelDefinition> allArmorModels() {
        return Collections.unmodifiableMap(armorModels);
    }

    public Optional<EntityModelDefinition> matchingFor(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        for (EntityModelDefinition definition : entityModels.values()) {
            if (definition.matches(entity)) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    private static EntityModelDefinition parseEntityDefinition(String modelId,
                                                               ConfigurationSection section,
                                                               Logger logger) {
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
            int raw = section.getInt("apply.attachment.custom_model_data");
            customModelData = validateCustomModelData(raw, ENTITY_CMD_MIN, ENTITY_CMD_MAX, logger,
                    "entity model", modelId);
        }

        Vector3f offset = parseVector(section, "apply.attachment.transform.offset", new Vector3f(0f, 0f, 0f));
        Vector3f rotation = parseVector(section, "apply.attachment.transform.rotation", new Vector3f(0f, 0f, 0f));
        Vector3f scale = parseVector(section, "apply.attachment.transform.scale", new Vector3f(1f, 1f, 1f));

        AttachmentSpec attachment = new AttachmentSpec(material, customModelData, offset, rotation, scale);
        return new EntityModelDefinition(modelId, entityType, condition, appearance, attachment);
    }

    private static ArmorModelDefinition parseArmorDefinition(String itemId,
                                                             ConfigurationSection section,
                                                             Logger logger) {
        String slotRaw = section.getString("slot", "");
        EquipmentSlot slot = parseEnum(EquipmentSlot.class, slotRaw);
        if (slot == null) {
            logger.warning("[Models] Unknown armor slot " + slotRaw + " for item " + itemId);
            return null;
        }

        List<Map<?, ?>> rawElements = section.getMapList("elements");
        if (rawElements == null || rawElements.isEmpty()) {
            logger.warning("[Models] Missing armor elements for item " + itemId);
            return null;
        }

        List<ArmorElement> elements = new ArrayList<>();
        int index = 0;
        for (Map<?, ?> raw : rawElements) {
            index++;
            ArmorElement element = parseArmorElement(itemId, raw, index, logger);
            if (element != null) {
                elements.add(element);
            }
        }
        if (elements.isEmpty()) {
            logger.warning("[Models] No valid armor elements for item " + itemId);
            return null;
        }

        return new ArmorModelDefinition(itemId, slot, List.copyOf(elements));
    }

    private static ArmorElement parseArmorElement(String itemId,
                                                  Map<?, ?> raw,
                                                  int index,
                                                  Logger logger) {
        if (raw == null) {
            return null;
        }
        String elementId = Objects.toString(raw.get("id"), "element_" + index);
        String itemKey = Objects.toString(raw.getOrDefault("item", "minecraft:stick"), "minecraft:stick");
        Material material = parseMaterial(itemKey);
        if (material == null) {
            logger.warning("[Models] Unknown material " + itemKey + " for armor " + itemId + ", using stick.");
            material = Material.STICK;
        }

        Integer customModelData = null;
        if (raw.containsKey("custom_model_data")) {
            Object cmdRaw = raw.get("custom_model_data");
            Integer parsed = parseInteger(cmdRaw);
            if (parsed != null) {
                customModelData = validateCustomModelData(parsed, ARMOR_CMD_MIN, ARMOR_CMD_MAX, logger,
                        "armor model", itemId);
            }
        }

        Map<?, ?> transform = raw.get("transform") instanceof Map<?, ?> map ? map : Map.of();
        Vector3f offset = parseVector(transform.get("offset"), new Vector3f(0f, 0f, 0f));
        Vector3f rotation = parseVector(transform.get("rotation"), new Vector3f(0f, 0f, 0f));
        Vector3f scale = parseVector(transform.get("scale"), new Vector3f(1f, 1f, 1f));

        AttachmentSpec attachment = new AttachmentSpec(material, customModelData, offset, rotation, scale);
        return new ArmorElement(elementId, attachment);
    }

    private static Integer validateCustomModelData(int value,
                                                   int min,
                                                   int max,
                                                   Logger logger,
                                                   String label,
                                                   String id) {
        if (value < min || value > max) {
            logger.warning("[Models] Custom model data " + value + " for " + label + " " + id
                    + " is outside of range " + min + "-" + max + "; ignoring.");
            return null;
        }
        return value;
    }

    private static Integer parseInteger(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private static Vector3f parseVector(Object raw, Vector3f fallback) {
        if (raw instanceof List<?> list && list.size() >= 3) {
            List<Float> values = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Object entry = list.get(i);
                if (entry instanceof Number number) {
                    values.add(number.floatValue());
                } else if (entry instanceof String str) {
                    try {
                        values.add(Float.parseFloat(str));
                    } catch (NumberFormatException ignored) {
                        return new Vector3f(fallback);
                    }
                } else {
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

    public record EntityModelDefinition(String id, EntityType entityType, ModelCondition condition,
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

    public record ArmorElement(String id, AttachmentSpec attachment) {
    }

    public record ArmorModelDefinition(String itemId, EquipmentSlot slot, List<ArmorElement> elements) {
    }
}
