package ru.realite.ui;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.menu.MenuListener;

public final class RealiteUIPlugin extends JavaPlugin {

    private MiniMessageMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        messages = new MiniMessageMessages(this, resolveLanguage());

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    public MiniMessageMessages messages() {
        return messages;
    }

    private String resolveLanguage() {
        String language = getConfig().getString("language");
        if (language == null || language.isBlank()) {
            language = getConfig().getString("lang", "ru");
        }
        return language;
    }

    private void saveIfNotExists(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + getDataFolder());
            return;
        }

        if (getResource(resourcePath) == null) {
            return;
        }

        if (!new java.io.File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }
}
