package ru.realite.items.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.models.ModelAsset;
import ru.realite.core.api.models.ModelAssetInfo;
import ru.realite.core.api.models.ModelAssetKind;
import ru.realite.core.api.models.ModelAssetRegistry;
import ru.realite.core.api.models.ModelOffset;
import ru.realite.core.api.models.ModelRendererHint;
import ru.realite.core.api.models.ModelVisualProfile;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModelAssetRegistryImpl implements ModelAssetRegistry {

    private final JavaPlugin plugin;
    private final Map<String, ModelAssetInfo> assets = new HashMap<>();

    public ModelAssetRegistryImpl(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        assets.clear();
        File dir = new File(plugin.getDataFolder(), "models");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Failed to create models directory: " + dir.getAbsolutePath());
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection root = yml.getConfigurationSection("models");
            if (root == null) {
                root = yml;
            }
            for (String key : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String modelId = section.getString("modelId", key);
                if (modelId == null || modelId.isBlank()) {
                    plugin.getLogger().warning("Model id missing in " + file.getName() + " for key " + key);
                    continue;
                }

                String kindRaw = section.getString("kind", "ENTITY");
                ModelAssetKind kind = parseKind(kindRaw);
                if (kind == null) {
                    plugin.getLogger().warning("Invalid kind '" + kindRaw + "' for model " + modelId + " in " + file.getName());
                    continue;
                }

                String rendererHintRaw = section.getString("rendererHint", "NONE");
                ModelRendererHint rendererHint = parseRendererHint(rendererHintRaw);
                if (rendererHint == null) {
                    plugin.getLogger().warning("Invalid rendererHint '" + rendererHintRaw + "' for model " + modelId + " in " + file.getName());
                    continue;
                }

                ModelVisualProfile visualProfile = parseVisualProfile(section.getConfigurationSection("visualProfile"));
                ModelAsset asset = new ModelAsset(modelId, kind, visualProfile, rendererHint);
                assets.put(modelId, new ModelAssetInfo(asset, file.getName()));
            }
        }

        plugin.getLogger().info("Loaded model assets: " + assets.size());
    }

    @Override
    public Optional<ModelAssetInfo> find(String modelId) {
        if (modelId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(assets.get(modelId));
    }

    @Override
    public Map<String, ModelAssetInfo> all() {
        return Collections.unmodifiableMap(assets);
    }

    private ModelVisualProfile parseVisualProfile(ConfigurationSection section) {
        if (section == null) {
            return new ModelVisualProfile(1.0, new ModelOffset(0.0, 0.0, 0.0), "ORIGIN");
        }
        double scale = section.getDouble("scale", 1.0);
        String anchor = section.getString("anchor", "ORIGIN");
        ConfigurationSection offsetSection = section.getConfigurationSection("offset");
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        if (offsetSection != null) {
            x = offsetSection.getDouble("x", 0.0);
            y = offsetSection.getDouble("y", 0.0);
            z = offsetSection.getDouble("z", 0.0);
        }
        return new ModelVisualProfile(scale, new ModelOffset(x, y, z), anchor);
    }

    private ModelAssetKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ModelAssetKind.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ModelRendererHint parseRendererHint(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ModelRendererHint.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
