package ru.realite.items;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import ru.realite.items.command.ItemsCommand;
import ru.realite.items.i18n.ItemMessages;
import ru.realite.items.listener.ItemRefreshListener;
import ru.realite.items.listener.ResourcePackListener;
import ru.realite.items.service.ItemRegistry;
import ru.realite.items.service.ItemService;
import ru.realite.core.api.logging.Banners;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class RealiteItemsPlugin extends JavaPlugin {

    private ItemMessages messages;
    private ItemRegistry registry;
    private ItemService itemService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("items/example_items.yml");
        saveIfNotExists("items/magic_items.yml");
        saveIfNotExists("items/familiars_items.yml");

        reloadAll();

        PluginCommand command = getCommand("ritems");
        if (command != null) {
            ItemsCommand executor = new ItemsCommand(itemService, messages, registry);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command /ritems not found in plugin.yml; executor not registered.");
        }

        Bukkit.getPluginManager().registerEvents(new ItemRefreshListener(this, itemService), this);
        Bukkit.getPluginManager().registerEvents(new ResourcePackListener(this, messages), this);
        Bukkit.getServicesManager().register(ItemService.class, itemService, this, ServicePriority.Normal);
        Banners.REALITE_ITEMS(this);
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
    }

    public void reloadAll() {
        reloadConfig();
        String lang = getConfig().getString("lang", "ru");
        this.messages = new ItemMessages(this, lang);
        this.registry = new ItemRegistry(this);
        this.registry.reload();
        this.itemService = new ItemService(this, registry, messages);
    }

    public ItemMessages messages() {
        return messages;
    }

    public ItemRegistry registry() {
        return registry;
    }

    public ItemService itemService() {
        return itemService;
    }

    public boolean isRefreshOnJoin() {
        return getConfig().getBoolean("items.refreshOnJoin", false);
    }

    private void saveIfNotExists(String resourcePath) {
        File outFile = new File(getDataFolder(), resourcePath);
        if (outFile.exists()) {
            return;
        }
        outFile.getParentFile().mkdirs();

        try (InputStream in = getResource(resourcePath)) {
            if (in == null) {
                getLogger().warning("Resource not found: " + resourcePath);
                return;
            }
            Files.copy(in, outFile.toPath());
        } catch (Exception e) {
            getLogger().severe("Failed to save resource: " + resourcePath);
            e.printStackTrace();
        }
    }
}
