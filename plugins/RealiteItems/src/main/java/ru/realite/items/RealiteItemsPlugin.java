package ru.realite.items;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.items.i18n.ItemsMessages;

public final class RealiteItemsPlugin extends JavaPlugin {

    private ItemsMessages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        messages = new ItemsMessages(this);
    }

    public ItemsMessages getMessages() {
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
