package ru.realite.core.i18n;

import java.io.File;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiniMessageMessages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String lang = "ru";

    public MiniMessageMessages(JavaPlugin plugin, String lang) {
        this.plugin = plugin;
        reload(lang);
    }

    public void reload(String lang) {
        this.lang = (lang == null || lang.isBlank()) ? "ru" : lang.trim().toLowerCase();
        File file = new File(plugin.getDataFolder(), "lang/messages_" + this.lang + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Messages file not found for lang '" + this.lang + "', falling back to ru");
            file = new File(plugin.getDataFolder(), "lang/messages_ru.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Возвращает сырую строку из messages yml (без десериализации).
     */
    public String raw(String key) {
        return messages.getString(key);
    }

    /**
     * raw с fallback.
     */
    public String rawOr(String key, String fallback) {
        String value = raw(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key);
        if (raw == null) {
            return Component.text("Missing message: " + key);
        }
        return deserialize(format(raw, placeholders));
    }

    private String format(String raw, Map<String, String> placeholders) {
        String prefix = messages.getString("format.prefix");
        if (prefix == null) {
            prefix = messages.getString("prefix", "");
        }
        String msg = raw.replace("{prefix}", prefix == null ? "" : prefix);
        if (placeholders != null) {
            for (var entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }

    private Component deserialize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }

        if (raw.indexOf('<') != -1 && raw.indexOf('>') != -1) {
            try {
                return MINI.deserialize(raw);
            } catch (Exception ignored) {
                // если кривая строка — упадём в legacy
            }
        }

        return LEGACY.deserialize(raw.replace('§', '&'));
    }
}
