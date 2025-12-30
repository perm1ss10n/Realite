package ru.realite.guilds.i18n;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuildMessages {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private YamlConfiguration messages = new YamlConfiguration();
    private String lang = "ru";

    public GuildMessages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String configured = plugin.getConfig().getString("language", "ru");
        String targetLang = configured == null || configured.isBlank() ? "ru" : configured.trim();
        lang = targetLang.toLowerCase(Locale.ROOT);
        File file = new File(plugin.getDataFolder(), "lang/messages_" + lang + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Messages file not found for lang '" + lang + "'. Falling back to ru.");
            file = new File(plugin.getDataFolder(), "lang/messages_ru.yml");
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public Component msg(String key, Map<String, String> placeholders) {
        String raw = messages.getString(key, "&cMissing message: &f" + key);
        if (raw == null) {
            raw = "";
        }
        String prefix = messages.getString("prefix", "");
        raw = raw.replace("{prefix}", prefix == null ? "" : prefix);
        for (var entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return LEGACY.deserialize(raw);
    }

    public Component msg(String key, String... placeholders) {
        return msg(key, toMap(placeholders));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(msg(key, placeholders));
    }

    public void send(Player player, String key, String... placeholders) {
        player.sendMessage(msg(key, placeholders));
    }

    private Map<String, String> toMap(String... placeholders) {
        Map<String, String> map = new HashMap<>();
        if (placeholders == null) {
            return map;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            map.put(placeholders[i], placeholders[i + 1]);
        }
        return map;
    }
}
