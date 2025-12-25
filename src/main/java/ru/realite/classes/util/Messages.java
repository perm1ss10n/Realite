package ru.realite.classes.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class Messages {

    private final JavaPlugin plugin;
    private YamlConfiguration yml;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        String raw = yml.getString(key, "&cMissing message: &f" + key);
        return Text.c(raw);
    }

    public String format(String key, Map<String, String> vars) {
        String msg = get(key);
        for (var e : vars.entrySet()) {
            msg = msg.replace("{" + e.getKey() + "}", e.getValue());
        }
        return msg;
    }
}
