package ru.realite.ui.settings;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSlot;

public final class UiSettingsStore {

    private final JavaPlugin plugin;
    private final Map<UUID, UiSettings> cache = new ConcurrentHashMap<>();

    public UiSettingsStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public UiSettings get(UUID playerId) {
        return cache.computeIfAbsent(playerId, this::load);
    }

    public void save(UUID playerId) {
        UiSettings settings = cache.get(playerId);
        if (settings == null) {
            return;
        }
        save(playerId, settings);
    }

    public void saveAll() {
        for (Map.Entry<UUID, UiSettings> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    private UiSettings load(UUID playerId) {
        UiSettings settings = new UiSettings();
        File file = file(playerId);
        if (!file.exists()) {
            return settings;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        loadSlot(yml, settings, UiSlot.BOSSBAR, "bossbar");
        loadSlot(yml, settings, UiSlot.ACTION_BAR, "actionbar");
        return settings;
    }

    private void loadSlot(YamlConfiguration yml, UiSettings settings, UiSlot slot, String key) {
        String raw = yml.getString(key);
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            settings.setProvider(slot, new UiProviderId(raw.trim()));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid UI provider id '" + raw + "' in " + key);
        }
    }

    private void save(UUID playerId, UiSettings settings) {
        File file = file(playerId);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            plugin.getLogger().warning("Failed to create UI settings folder: " + parent);
            return;
        }
        YamlConfiguration yml = new YamlConfiguration();
        writeSlot(yml, settings, UiSlot.BOSSBAR, "bossbar");
        writeSlot(yml, settings, UiSlot.ACTION_BAR, "actionbar");
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save UI settings for " + playerId + ": " + ex.getMessage());
        }
    }

    private void writeSlot(YamlConfiguration yml, UiSettings settings, UiSlot slot, String key) {
        settings.provider(slot).map(UiProviderId::value).ifPresent(value -> yml.set(key, value));
    }

    private File file(UUID playerId) {
        return new File(plugin.getDataFolder(), "players/" + playerId + ".yml");
    }
}
