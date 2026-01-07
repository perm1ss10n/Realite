package ru.realite.magic;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;

public final class RealiteMagicPlugin extends JavaPlugin {

    private MagicMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        messages = new MagicMessages(this);
    }

    public MagicMessages getMessages() {
        return messages;
    }

    private void saveIfNotExists(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + getDataFolder());
            return;
        }

        if (getResource(resourcePath) == null) {
            return;
        }

        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }
}
