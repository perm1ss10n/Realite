package ru.realite.items;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import ru.realite.core.api.CoreApi;
import ru.realite.items.command.ItemsCommand;
import ru.realite.items.i18n.ItemMessages;
import ru.realite.items.listener.ItemRefreshListener;
import ru.realite.items.listener.ResourcePackListener;
import ru.realite.items.service.ItemRegistry;
import ru.realite.items.service.ItemService;
import ru.realite.items.service.ModelAssetRegistryImpl;
import ru.realite.core.api.logging.Banners;
import ru.realite.core.api.models.ModelAssetRegistry;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public final class RealiteItemsPlugin extends JavaPlugin {

    private ItemMessages messages;
    private ItemRegistry registry;
    private ItemService itemService;
    private ModelAssetRegistryImpl modelAssetRegistry;
    private BukkitTask corePollingTask;
    private boolean modelRegistryRegistered;
    private boolean waitingLogged;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("items/example_items.yml");
        saveIfNotExists("items/magic_items.yml");
        saveIfNotExists("items/familiars_items.yml");
        saveIfNotExists("models/example_models.yml");

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
        registerModelRegistry();
        Banners.REALITE_ITEMS(this);
    }

    @Override
    public void onDisable() {
        if (corePollingTask != null) {
            corePollingTask.cancel();
            corePollingTask = null;
        }
        unregisterModelRegistry();
        Bukkit.getServicesManager().unregisterAll(this);
    }

    public void reloadAll() {
        reloadConfig();
        String lang = getConfig().getString("lang", "ru");
        this.messages = new ItemMessages(this, lang);
        this.registry = new ItemRegistry(this);
        this.registry.reload();
        this.itemService = new ItemService(this, registry, messages);
        this.modelAssetRegistry = new ModelAssetRegistryImpl(this);
        this.modelAssetRegistry.reload();
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

    public ModelAssetRegistry modelAssetRegistry() {
        return modelAssetRegistry;
    }

    public boolean isRefreshOnJoin() {
        return getConfig().getBoolean("items.refreshOnJoin", false);
    }

    private void registerModelRegistry() {
        if (attemptRegisterModelRegistry()) {
            return;
        }
        if (corePollingTask != null) {
            return;
        }
        corePollingTask = getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    if (attemptRegisterModelRegistry()) {
                        corePollingTask.cancel();
                        corePollingTask = null;
                    }
                },
                20L,
                40L
        );
    }

    private boolean attemptRegisterModelRegistry() {
        CoreApi core = resolveCore();
        if (core == null) {
            if (!waitingLogged) {
                waitingLogged = true;
                getLogger().info("CoreApi not ready for model registry, waiting for registration.");
            }
            return false;
        }
        boolean registered = core.services().registerIfAbsent(ModelAssetRegistry.class, modelAssetRegistry);
        if (registered) {
            modelRegistryRegistered = true;
            getLogger().info("ModelAssetRegistry registered in Core.");
            return true;
        }
        ModelAssetRegistry current = core.services().get(ModelAssetRegistry.class);
        if (current == modelAssetRegistry) {
            modelRegistryRegistered = true;
            return true;
        }
        if (!modelRegistryRegistered) {
            getLogger().warning("ModelAssetRegistry already registered by another module.");
        }
        return true;
    }

    private void unregisterModelRegistry() {
        if (!modelRegistryRegistered) {
            return;
        }
        CoreApi core = resolveCore();
        if (core == null) {
            return;
        }
        ModelAssetRegistry current = core.services().get(ModelAssetRegistry.class);
        if (current == modelAssetRegistry) {
            core.services().unregister(ModelAssetRegistry.class);
        }
        modelRegistryRegistered = false;
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
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
