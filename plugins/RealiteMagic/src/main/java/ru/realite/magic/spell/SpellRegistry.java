package ru.realite.magic.spell;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.target.SpellTargetDefinition;
import ru.realite.magic.target.SpellTargetType;
import ru.realite.magic.validation.SchemaError;
import ru.realite.magic.validation.SchemaReport;
import ru.realite.magic.validation.SpellSchemaValidator;

public final class SpellRegistry {

    private final JavaPlugin plugin;
    private final EffectExecutorRegistry effectRegistry;
    private final Map<String, SpellDefinition> spells = new HashMap<>();

    public SpellRegistry(JavaPlugin plugin, EffectExecutorRegistry effectRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.effectRegistry = Objects.requireNonNull(effectRegistry, "effectRegistry");
    }

    public SpellLoadReport load() {
        return reloadInternal(true);
    }

    public SpellLoadReport reload() {
        return load();
    }

    public SpellLoadReport validate() {
        return reloadInternal(false);
    }

    private SpellLoadReport reloadInternal(boolean applyChanges) {
        List<SpellLoadError> errors = new ArrayList<>();
        Map<String, SpellDefinition> loaded = new HashMap<>();
        File folder = new File(plugin.getDataFolder(), "spells");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                errors.add(SpellLoadError.ofKey(
                        "spells",
                        null,
                        "magic.cmd.spells.errors.folder_create_failed",
                        Map.of("path", folder.getAbsolutePath())));
                return new SpellLoadReport(0, errors);
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return new SpellLoadReport(0, errors);
        }

        Material defaultIconMaterial = resolveDefaultIconMaterial(errors);
        SpellSchemaValidator validator = new SpellSchemaValidator(effectRegistry);
        for (File file : files) {
            YamlConfiguration cfg = new YamlConfiguration();
            try {
                cfg.load(file);
            } catch (IOException | InvalidConfigurationException ex) {
                addSchemaError(errors, file.getName(), null, "yaml",
                        "magic.cmd.spells.errors.invalid_value",
                        Map.of("field", "yaml", "value", String.valueOf(ex.getMessage())));
                continue;
            }
            ConfigurationSection root = cfg.getConfigurationSection("spell");
            if (root == null) {
                addSchemaError(errors, file.getName(), null, "spell",
                        "magic.cmd.spells.errors.missing_field",
                        Map.of("field", "spell"));
                continue;
            }
            SpellDefinition def = parseSpell(root, defaultIconMaterial);
            SchemaReport schemaReport = validator.validate(def, file.getName());
            addSchemaErrors(errors, schemaReport);
            if (!schemaReport.ok()) {
                continue;
            }
            if (loaded.containsKey(def.id())) {
                addSchemaError(errors, file.getName(), def.id(), "id",
                        "magic.cmd.spells.errors.duplicate_id",
                        Map.of("id", def.id()));
                continue;
            }
            loaded.put(def.id(), def);
        }

        if (applyChanges) {
            spells.clear();
            spells.putAll(loaded);
        }

        return new SpellLoadReport(loaded.size(), errors);
    }

    public Collection<SpellDefinition> all() {
        return Collections.unmodifiableCollection(spells.values());
    }

    public SpellDefinition get(String id) {
        return spells.get(id);
    }

    public Optional<SpellDefinition> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(spells.get(id));
    }

    private SpellDefinition parseSpell(ConfigurationSection section,
                                       Material defaultIconMaterial) {
        String id = section.getString("id");
        String typeRaw = section.getString("type");
        SpellType type = parseSpellType(typeRaw);
        String nameKey = section.getString("nameKey");
        String descKey = section.getString("descKey");
        double mana = section.getDouble("mana", -1);
        long cooldownTicks = section.getLong("cooldownTicks", -1);
        double range = section.getDouble("range", -1);
        double damage = section.getDouble("damage", -1);

        SpellRequirements requirements = parseRequirements(section.getConfigurationSection("requirements"));
        SpellTargetDefinition target = parseTarget(section.getConfigurationSection("target"), range);
        List<SpellEffectDefinition> effects = parseEffects(section);
        SpellCastTrigger castTrigger = parseCastTrigger(section.getConfigurationSection("cast"));
        String castItemId = parseCastItemId(section.getConfigurationSection("cast"));
        SpellGiveItem giveItem = parseGiveItem(section.getConfigurationSection("effects"));
        Material iconMaterial = parseIconMaterial(section.getConfigurationSection("icon"),
                defaultIconMaterial);
        Integer iconCustomModelData = parseIconCustomModelData(section.getConfigurationSection("icon"));
        Integer guiSlot = parseGuiSlot(section.getConfigurationSection("gui"));
        return new SpellDefinition(id, type, nameKey, descKey, mana, cooldownTicks, range, damage, requirements,
                target, effects, castTrigger, castItemId, giveItem.id(), giveItem.amount(), iconMaterial,
                iconCustomModelData, guiSlot);
    }

    private List<SpellEffectDefinition> parseEffects(ConfigurationSection section) {
        List<Map<?, ?>> rawEffects = section.getMapList("effects");
        if (rawEffects == null || rawEffects.isEmpty()) {
            return List.of();
        }
        List<SpellEffectDefinition> effects = new ArrayList<>();
        for (Object raw : rawEffects) {
            if (!(raw instanceof Map<?, ?> effectMap)) {
                effects.add(null);
                continue;
            }
            Object typeRaw = effectMap.get("type");
            String type = typeRaw == null ? "" : String.valueOf(typeRaw);
            Map<String, Object> params = new HashMap<>();
            for (Map.Entry<?, ?> entry : effectMap.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey());
                if ("type".equalsIgnoreCase(key)) {
                    continue;
                }
                params.put(key, entry.getValue());
            }
            effects.add(new SpellEffectDefinition(type, params));
        }
        return List.copyOf(effects);
    }

    private SpellRequirements parseRequirements(ConfigurationSection section) {
        if (section == null) {
            return new SpellRequirements(null, null, null, false);
        }
        String classId = section.getString("class");
        String evolutionId = section.getString("evolution");
        String requiredItemId = section.getString("requiredItemId");
        boolean consumeOnCast = section.getBoolean("consumeOnCast", false);
        if (classId != null && classId.isBlank()) {
            classId = null;
        }
        if (evolutionId != null && evolutionId.isBlank()) {
            evolutionId = null;
        }
        if (requiredItemId != null && requiredItemId.isBlank()) {
            requiredItemId = null;
        }
        return new SpellRequirements(classId, evolutionId, requiredItemId, consumeOnCast);
    }

    private SpellCastTrigger parseCastTrigger(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String triggerRaw = section.getString("trigger");
        if (triggerRaw == null || triggerRaw.isBlank()) {
            return null;
        }
        try {
            return SpellCastTrigger.valueOf(triggerRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String parseCastItemId(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String castItemId = section.getString("itemId");
        if (castItemId != null && castItemId.isBlank()) {
            return null;
        }
        return castItemId;
    }

    private SpellGiveItem parseGiveItem(ConfigurationSection section) {
        if (section == null) {
            return new SpellGiveItem(null, 1);
        }
        ConfigurationSection giveItemSection = section.getConfigurationSection("giveItem");
        if (giveItemSection == null) {
            return new SpellGiveItem(null, 1);
        }
        String id = giveItemSection.getString("id");
        if (id != null && id.isBlank()) {
            id = null;
        }
        int amount = giveItemSection.getInt("amount", 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new SpellGiveItem(id, amount);
    }

    private SpellTargetDefinition parseTarget(ConfigurationSection section, double range) {
        if (section == null) {
            return null;
        }
        String typeRaw = section.getString("type");
        SpellTargetType type = null;
        if (typeRaw != null && !typeRaw.isBlank()) {
            try {
                type = SpellTargetType.valueOf(typeRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                type = null;
            }
        }
        double maxDistance = section.getDouble("maxDistance", range);
        boolean lineOfSight = section.getBoolean("lineOfSight", true);
        boolean allowPlayers = section.getBoolean("allowPlayers", true);
        boolean allowMobs = section.getBoolean("allowMobs", true);
        return new SpellTargetDefinition(type, maxDistance, lineOfSight, allowPlayers, allowMobs);
    }

    private Material resolveDefaultIconMaterial(List<SpellLoadError> errors) {
        String configured = plugin.getConfig().getString("menu.spellSelect.defaultSpellIconMaterial", "PAPER");
        if (configured == null || configured.isBlank()) {
            configured = "PAPER";
        }
        Material material = Material.matchMaterial(configured.trim());
        if (material == null) {
            errors.add(SpellLoadError.ofKey(
                    "config.yml",
                    null,
                    "magic.cmd.spells.errors.invalid_default_icon_material",
                    Map.of("material", configured)));
            return Material.PAPER;
        }
        return material;
    }

    private Material parseIconMaterial(ConfigurationSection section,
                                       Material defaultMaterial) {
        if (section == null) {
            return defaultMaterial;
        }
        String materialName = section.getString("material");
        if (materialName == null || materialName.isBlank()) {
            return defaultMaterial;
        }
        Material material = Material.matchMaterial(materialName.trim());
        return material == null ? defaultMaterial : material;
    }

    private Integer parseIconCustomModelData(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        if (!section.isSet("customModelData")) {
            return null;
        }
        int value = section.getInt("customModelData");
        return value >= 0 ? value : null;
    }

    private Integer parseGuiSlot(ConfigurationSection section) {
        if (section == null || !section.isSet("slot")) {
            return null;
        }
        int slot = section.getInt("slot");
        return slot < 0 ? null : slot;
    }

    private record SpellGiveItem(String id, int amount) {
    }

    private SpellType parseSpellType(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) {
            return null;
        }
        try {
            return SpellType.valueOf(typeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void addSchemaErrors(List<SpellLoadError> errors, SchemaReport report) {
        for (SchemaError error : report.errors()) {
            addSchemaError(errors, error.file(), error.spellId(), error.path(), error.messageKey(), error.placeholders());
        }
    }

    private void addSchemaError(List<SpellLoadError> errors,
                                String fileName,
                                String spellId,
                                String path,
                                String messageKey,
                                Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>(placeholders == null ? Map.of() : placeholders);
        if (path != null) {
            merged.put("path", path);
        }
        errors.add(SpellLoadError.ofKey(fileName, spellId, messageKey, merged));
    }
}
