package ru.realite.chat;

import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class ChatMessages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

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
            plugin.getLogger().warning("Messages file not found for lang '" + this.lang + "', falling back to ru");
            file = new File(plugin.getDataFolder(), "lang/messages_ru.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    Component get(String key) {
        String raw = messages.getString(key);
        if (raw == null) {
            return Component.text("Missing message: " + key);
        }
        return deserialize(raw);
    }

    private Component deserialize(String raw) {
        if (raw == null || raw.isEmpty())
            return Component.empty();

        // Если похоже на MiniMessage — парсим MiniMessage
        // (почти все ваши строки типа <green>... именно такие)
        if (raw.indexOf('<') != -1 && raw.indexOf('>') != -1) {
            try {
                return MINI.deserialize(raw);
            } catch (Exception ignored) {
                // Если кривая строка — упадём в legacy
            }
        }

        // Иначе legacy (&/§)
        return LEGACY.deserialize(raw.replace('§', '&'));
    }
}
