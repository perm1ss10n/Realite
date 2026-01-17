package ru.realite.familiars.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.Objects;

public final class Messages {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final YamlConfiguration config;

    public Messages(YamlConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public Component get(String key) {
        return get(key, Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        String raw = config.getString(key);
        if (raw == null) {
            return Component.text("Missing message: " + key);
        }
        String prefix = config.getString("prefix", "");
        String formatted = raw.replace("{prefix}", prefix == null ? "" : prefix);
        for (var entry : placeholders.entrySet()) {
            formatted = formatted.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return MINI.deserialize(formatted);
    }

    public String raw(String key) {
        return config.getString(key);
    }
}
