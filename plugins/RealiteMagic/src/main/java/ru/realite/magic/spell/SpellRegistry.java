package ru.realite.magic.spell;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpellRegistry {

    private final JavaPlugin plugin;
    private final Map<String, SpellDefinition> spells = new HashMap<>();

    public SpellRegistry(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void load() {
        spells.clear();
        File folder = new File(plugin.getDataFolder(), "spells");
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                plugin.getLogger().warning("Failed to create spells folder: " + folder.getAbsolutePath());
                return;
            }
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = cfg.getConfigurationSection("spell");
            if (root == null) {
                plugin.getLogger().warning("Spell file " + file.getName() + " missing 'spell' section. Skipping.");
                continue;
            }
            SpellDefinition def = parseSpell(file.getName(), root);
            if (def != null) {
                if (spells.containsKey(def.id())) {
                    plugin.getLogger().warning("Duplicate spell id '" + def.id() + "' in " + file.getName() + ". Skipping.");
                    continue;
                }
                spells.put(def.id(), def);
            }
        }
    }

    public Collection<SpellDefinition> all() {
        return Collections.unmodifiableCollection(spells.values());
    }

    public SpellDefinition get(String id) {
        return spells.get(id);
    }

    private SpellDefinition parseSpell(String fileName, ConfigurationSection section) {
        String id = section.getString("id");
        String typeRaw = section.getString("type");
        String nameKey = section.getString("nameKey");
        String descKey = section.getString("descKey");
        double mana = section.getDouble("mana", -1);
        long cooldownTicks = section.getLong("cooldownTicks", -1);
        double range = section.getDouble("range", -1);
        double damage = section.getDouble("damage", -1);
        SpellRequirements requirements = parseRequirements(section.getConfigurationSection("requirements"));

        if (id == null || id.isBlank()) {
            plugin.getLogger().warning("Spell file " + fileName + " has invalid id. Skipping.");
            return null;
        }
        if (typeRaw == null || typeRaw.isBlank()) {
            plugin.getLogger().warning("Spell '" + id + "' has missing type in " + fileName + ". Skipping.");
            return null;
        }
        SpellType type;
        try {
            type = SpellType.valueOf(typeRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Spell '" + id + "' has unknown type '" + typeRaw + "' in " + fileName + ". Skipping.");
            return null;
        }
        if (nameKey == null || nameKey.isBlank() || descKey == null || descKey.isBlank()) {
            plugin.getLogger().warning("Spell '" + id + "' missing nameKey/descKey in " + fileName + ". Skipping.");
            return null;
        }
        if (mana < 0 || cooldownTicks < 0 || range <= 0 || damage < 0) {
            plugin.getLogger().warning("Spell '" + id + "' has invalid numbers in " + fileName + ". Skipping.");
            return null;
        }
        return new SpellDefinition(id, type, nameKey, descKey, mana, cooldownTicks, range, damage, requirements);
    }

    private SpellRequirements parseRequirements(ConfigurationSection section) {
        if (section == null) {
            return new SpellRequirements(null, null);
        }
        String classId = section.getString("class");
        String evolutionId = section.getString("evolution");
        if (classId != null && classId.isBlank()) {
            classId = null;
        }
        if (evolutionId != null && evolutionId.isBlank()) {
            evolutionId = null;
        }
        return new SpellRequirements(classId, evolutionId);
    }
}
