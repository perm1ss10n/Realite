package ru.realite.items.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ItemMessages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private YamlConfiguration messages;
    private String lang = "ru";
    private String prefix = "";

    public ItemMessages(JavaPlugin plugin, String lang) {
        this.plugin = plugin;
        reload(lang);
    }

    public void reload(String lang) {
        this.lang = (lang == null || lang.isBlank()) ? "ru" : lang.trim().toLowerCase();

        File file = new File(plugin.getDataFolder(), "lang/messages_" + this.lang + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Messages file not found for lang '" + this.lang + "': " + file.getName() + ". Falling back to ru.");
            file = new File(plugin.getDataFolder(), "lang/messages_ru.yml");
        }

        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = messages.getString("format.prefix", "");
    }

    public Component get(String key, String def) {
        String raw = messages.getString(key, def);
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        return LEGACY.deserialize(applyVars(raw, null));
    }

    public List<Component> getList(String key) {
        if (messages == null) {
            return Collections.emptyList();
        }
        List<String> list = messages.getStringList(key);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(line -> LEGACY.deserialize(applyVars(line, null)))
                .collect(Collectors.toList());
    }

    public void send(CommandSender sender, String key, String def) {
        sender.sendMessage(get(key, def));
    }

    public void send(CommandSender sender, String key, String def, Map<String, String> vars) {
        String raw = messages.getString(key, def);
        if (raw == null || raw.isBlank()) {
            sender.sendMessage(Component.empty());
            return;
        }
        sender.sendMessage(LEGACY.deserialize(applyVars(raw, vars)));
    }

    private String applyVars(String raw, Map<String, String> vars) {
        String msg = raw;
        if (prefix != null) {
            msg = msg.replace("{prefix}", prefix);
        }
        if (vars != null) {
            for (var entry : vars.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return msg;
    }
}
