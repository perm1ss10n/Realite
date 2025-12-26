package ru.realite.classes.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Messages {

    private final JavaPlugin plugin;
    private YamlConfiguration yml;
    private String lang = "ru";

    public Messages(JavaPlugin plugin, String lang) {
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

        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    // совместимость, если ты где-то зовёшь messages.reload()
    public void reload() {
        reload(this.lang);
    }

    public String get(String key) {
        String raw = yml.getString(key, "&cMissing message: &f" + key);
        return Text.c(raw);
    }

    public List<String> getList(String key) {
        if (yml == null) return Collections.emptyList();
        List<String> list = yml.getStringList(key);
        if (list == null) return Collections.emptyList();
        return list.stream().map(Text::c).toList();
    }

    public String format(String key, Map<String, String> vars) {
        return formatLine(get(key), vars);
    }

    public String formatLine(String line, Map<String, String> vars) {
        String msg = line;
        if (vars != null) {
            for (var e : vars.entrySet()) {
                msg = msg.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return msg;
    }
}
