package ru.realite.chat;

import java.io.File;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class ChatMessages {
    private final JavaPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String lang = "ru";

    ChatMessages(JavaPlugin plugin, String lang) {
        this.plugin = plugin;
        reload(lang);
    }

    void reload(String lang) {
        this.lang = (lang == null || lang.isBlank()) ? "ru" : lang.trim().toLowerCase();
        File file = new File(plugin.getDataFolder(), "lang/messages_" + this.lang + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Messages file not found for lang '" + this.lang + "': " + file.getName()
                    + ". Falling back to ru.");
            file = new File(plugin.getDataFolder(), "lang/messages_ru.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    String get(String key) {
        String raw = messages.getString(key, "&cMissing message: &f" + key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
