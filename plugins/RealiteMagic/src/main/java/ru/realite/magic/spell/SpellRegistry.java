package ru.realite.magic.spell;

import java.io.File;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.EffectTargetType;
import ru.realite.magic.effect.EffectValidationResult;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.effect.SpellEffectExecutor;
import ru.realite.magic.target.SpellTargetDefinition;
import ru.realite.magic.target.SpellTargetType;

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
        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = cfg.getConfigurationSection("spell");
            if (root == null) {
                errors.add(SpellLoadError.ofKey(
                        file.getName(),
                        null,
                        "magic.cmd.spells.errors.missing_section",
                        Map.of()));
                continue;
            }
            SpellDefinition def = parseSpell(file.getName(), root, defaultIconMaterial, errors);
            if (def != null) {
                if (loaded.containsKey(def.id())) {
                    errors.add(SpellLoadError.ofKey(
                            file.getName(),
                            def.id(),
                            "magic.cmd.spells.errors.duplicate_id",
                            Map.of("id", def.id())));
                    continue;
                }
                loaded.put(def.id(), def);
            }
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

    private SpellDefinition parseSpell(String fileName,
                                       ConfigurationSection section,
                                       Material defaultIconMaterial,
                                       List<SpellLoadError> errors) {
        String id = section.getString("id");
        if (id == null || id.isBlank()) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    null,
                    "magic.cmd.spells.errors.invalid_id",
                    Map.of()));
            return null;
        }
        String typeRaw = section.getString("type");
        if (typeRaw == null || typeRaw.isBlank()) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    id,
                    "magic.cmd.spells.errors.missing_type",
                    Map.of()));
            return null;
        }
        SpellType type;
        try {
            type = SpellType.valueOf(typeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    id,
                    "magic.cmd.spells.errors.unknown_type",
                    Map.of("type", typeRaw)));
            return null;
        }
        String nameKey = section.getString("nameKey");
        String descKey = section.getString("descKey");
        double mana = section.getDouble("mana", -1);
        long cooldownTicks = section.getLong("cooldownTicks", -1);
        double range = section.getDouble("range", -1);
        double damage = section.getDouble("damage", -1);
        if (nameKey == null || nameKey.isBlank() || descKey == null || descKey.isBlank()) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    id,
                    "magic.cmd.spells.errors.missing_name_desc",
                    Map.of()));
            return null;
        }
        if (mana < 0 || cooldownTicks < 0 || range <= 0 || damage < 0) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    id,
                    "magic.cmd.spells.errors.invalid_numbers",
                    Map.of()));
            return null;
        }

        SpellRequirements requirements = parseRequirements(section.getConfigurationSection("requirements"));
        SpellTargetDefinition target = parseTarget(section.getConfigurationSection("target"), range);
        List<SpellEffectDefinition> effects = parseEffects(section, target, fileName, id, errors);
        String castItemId = parseCastItemId(section.getConfigurationSection("cast"));
        SpellGiveItem giveItem = parseGiveItem(section.getConfigurationSection("effects"));
        Material iconMaterial = parseIconMaterial(section.getConfigurationSection("icon"),
                defaultIconMaterial,
                id,
                fileName,
                errors);
        Integer iconCustomModelData = parseIconCustomModelData(section.getConfigurationSection("icon"));
        Integer guiSlot = parseGuiSlot(section.getConfigurationSection("gui"), id, fileName, errors);
        return new SpellDefinition(id, type, nameKey, descKey, mana, cooldownTicks, range, damage, requirements,
                target, effects, castItemId, giveItem.id(), giveItem.amount(), iconMaterial, iconCustomModelData, guiSlot);
    }

    private List<SpellEffectDefinition> parseEffects(ConfigurationSection section,
                                                     SpellTargetDefinition target,
                                                     String fileName,
                                                     String spellId,
                                                     List<SpellLoadError> errors) {
        List<Map<?, ?>> rawEffects = section.getMapList("effects");
        if (rawEffects == null || rawEffects.isEmpty()) {
            return List.of();
        }
        List<SpellEffectDefinition> effects = new ArrayList<>();
        int index = 0;
        for (Object raw : rawEffects) {
            int effectIndex = ++index;
            if (!(raw instanceof Map<?, ?> effectMap)) {
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        "magic.cmd.spells.errors.effect_invalid_entry",
                        Map.of("index", String.valueOf(effectIndex))));
                continue;
            }
            Object typeRaw = effectMap.get("type");
            if (typeRaw == null || String.valueOf(typeRaw).isBlank()) {
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        "magic.cmd.spells.errors.effect_missing_type",
                        Map.of("index", String.valueOf(effectIndex))));
                continue;
            }
            String type = String.valueOf(typeRaw);
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
            SpellEffectDefinition definition;
            try {
                definition = new SpellEffectDefinition(type, params);
            } catch (IllegalArgumentException ex) {
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        "magic.cmd.spells.errors.effect_missing_type",
                        Map.of("index", String.valueOf(effectIndex))));
                continue;
            }
            SpellEffectExecutor executor = effectRegistry.find(definition.type()).orElse(null);
            if (executor == null) {
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        "magic.cmd.spells.errors.effect_unknown_type",
                        Map.of("index", String.valueOf(effectIndex), "type", definition.type())));
                continue;
            }
            EffectValidationResult validation = executor.validate(definition);
            if (!validation.isValid()) {
                Map<String, String> placeholders = new HashMap<>(validation.placeholders());
                placeholders.putIfAbsent("index", String.valueOf(effectIndex));
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        validation.messageKey(),
                        placeholders));
                continue;
            }
            EffectTargetType effectTarget = EffectTargetType.from(definition.params().get("target"));
            if (!isTargetCompatible(target, effectTarget)) {
                errors.add(SpellLoadError.ofKey(
                        fileName,
                        spellId,
                        "magic.cmd.spells.errors.effect_target_mismatch",
                        Map.of("index", String.valueOf(effectIndex),
                                "type", definition.type(),
                                "target", String.valueOf(effectTarget),
                                "spellTarget", String.valueOf(target.type()))));
                continue;
            }
            effects.add(definition);
        }
        return List.copyOf(effects);
    }

    private boolean isTargetCompatible(SpellTargetDefinition target, EffectTargetType effectTarget) {
        if (target == null || effectTarget == null) {
            return false;
        }
        SpellTargetType spellTargetType = target.type();
        if (spellTargetType == null) {
            return false;
        }
        return switch (effectTarget) {
            case ENTITY -> spellTargetType == SpellTargetType.ENTITY || spellTargetType == SpellTargetType.SELF;
            case LOCATION -> spellTargetType == SpellTargetType.LOCATION
                    || spellTargetType == SpellTargetType.BLOCK
                    || spellTargetType == SpellTargetType.ENTITY
                    || spellTargetType == SpellTargetType.SELF
                    || spellTargetType == SpellTargetType.NONE;
        };
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
            return SpellTargetDefinition.none();
        }
        String typeRaw = section.getString("type");
        SpellTargetType type = SpellTargetType.NONE;
        if (typeRaw != null && !typeRaw.isBlank()) {
            try {
                type = SpellTargetType.valueOf(typeRaw.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                type = SpellTargetType.NONE;
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
                                       Material defaultMaterial,
                                       String spellId,
                                       String fileName,
                                       List<SpellLoadError> errors) {
        if (section == null) {
            return defaultMaterial;
        }
        String materialName = section.getString("material");
        if (materialName == null || materialName.isBlank()) {
            return defaultMaterial;
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    spellId,
                    "magic.cmd.spells.errors.invalid_icon_material",
                    Map.of("material", materialName)));
            return defaultMaterial;
        }
        return material;
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

    private Integer parseGuiSlot(ConfigurationSection section,
                                 String spellId,
                                 String fileName,
                                 List<SpellLoadError> errors) {
        if (section == null || !section.isSet("slot")) {
            return null;
        }
        int slot = section.getInt("slot");
        if (slot < 0) {
            errors.add(SpellLoadError.ofKey(
                    fileName,
                    spellId,
                    "magic.cmd.spells.errors.invalid_gui_slot",
                    Map.of("slot", String.valueOf(slot))));
            return null;
        }
        return slot;
    }

    private record SpellGiveItem(String id, int amount) {
    }
}
