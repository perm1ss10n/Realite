package ru.realite.items.service;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import ru.realite.items.model.ItemDefinition;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ItemRegistry {

    private final JavaPlugin plugin;
    private final Map<String, ItemDefinition> items = new HashMap<>();

    public ItemRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        items.clear();
        File dir = new File(plugin.getDataFolder(), "items");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Failed to create items directory: " + dir.getAbsolutePath());
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yml.getConfigurationSection("items");
            if (root == null) {
                root = yml;
            }
            for (String key : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String id = section.getString("id", key);
                if (id == null || id.isBlank()) {
                    plugin.getLogger().warning("Item id missing in " + file.getName() + " for key " + key);
                    continue;
                }

                String materialName = section.getString("material");
                if (materialName == null || materialName.isBlank()) {
                    plugin.getLogger().warning("Material missing for item " + id + " in " + file.getName());
                    continue;
                }

                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Invalid material '" + materialName + "' for item " + id + " in " + file.getName());
                    continue;
                }

                Integer customModelData = section.contains("customModelData")
                        ? section.getInt("customModelData")
                        : null;
                String nameKey = section.getString("nameKey", "");
                var loreKeys = section.getStringList("loreKeys");
                boolean glow = section.getBoolean("glow", false);
                boolean unstackable = section.getBoolean("unstackable", false);
                Map<String, Object> tags = Map.of();
                ConfigurationSection tagsSection = section.getConfigurationSection("tags");
                if (tagsSection != null) {
                    tags = new HashMap<>(tagsSection.getValues(false));
                }

                ItemDefinition def = new ItemDefinition(
                        id,
                        material,
                        customModelData,
                        nameKey,
                        loreKeys,
                        glow,
                        unstackable,
                        tags
                );
                items.put(id, def);
            }
        }

        plugin.getLogger().info("Loaded items: " + items.size());
    }

    public Optional<ItemDefinition> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(items.get(id));
    }

    public Map<String, ItemDefinition> items() {
        return Collections.unmodifiableMap(items);
    }
}
