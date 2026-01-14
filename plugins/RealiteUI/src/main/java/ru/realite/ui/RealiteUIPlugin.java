package ru.realite.ui;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Subscription;
import ru.realite.core.api.ui.UiInvalidateEvent;
import ru.realite.core.api.ui.UiPaginationService;
import ru.realite.core.api.ui.UiRegistry;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.command.UiCommand;
import ru.realite.ui.hud.UiHudService;
import ru.realite.ui.menu.MenuListener;
import ru.realite.ui.pagination.UiPaginationServiceImpl;
import ru.realite.ui.screen.UiScreenRegistryImpl;
import ru.realite.ui.settings.UiSettingsStore;

public final class RealiteUIPlugin extends JavaPlugin {

    private MiniMessageMessages messages;
    private UiSettingsStore settingsStore;
    private UiHudService hudService;
    private Subscription uiSubscription;
    private BukkitTask pollingTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        messages = new MiniMessageMessages(this, resolveLanguage());
        settingsStore = new UiSettingsStore(this);

        CoreApi core = resolveCore();
        if (core == null) {
            getLogger().warning("CoreApi service not found. UI settings are disabled.");
            // UI без CoreApi не работает — можно просто выйти
            getServer().getPluginManager().registerEvents(new MenuListener(), this);
            return;
        }

        core.services().registerIfAbsent(UiScreenRegistry.class, new UiScreenRegistryImpl());
        core.services().registerIfAbsent(UiPaginationService.class, new UiPaginationServiceImpl());

        UiRegistry registry = core.services().get(UiRegistry.class);
        if (registry == null) {
            getLogger().warning("UiRegistry not found. UI settings will be unavailable.");
            getServer().getPluginManager().registerEvents(new MenuListener(), this);
            return;
        }

        hudService = new UiHudService(this, messages, registry, settingsStore);
        getServer().getPluginManager().registerEvents(hudService, this);

        uiSubscription = core.events().subscribe(
                UiInvalidateEvent.class,
                event -> hudService.refreshIfMatches(event.player(), event.providerId()));

        long pollingTicks = getConfig().getLong("hud.pollingTicks", 40L);
        if (pollingTicks > 0) {
            pollingTask = getServer().getScheduler().runTaskTimer(
                    this, hudService::refreshOnline, pollingTicks, pollingTicks);
        }

        var cmd = getCommand("ui");
        if (cmd != null) {
            cmd.setExecutor(new UiCommand(messages, settingsStore, hudService, registry));
        }

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    @Override
    public void onDisable() {
        if (uiSubscription != null) {
            uiSubscription.unsubscribe();
            uiSubscription = null;
        }
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        if (settingsStore != null) {
            settingsStore.saveAll();
        }
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

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            getLogger().warning("CoreApi service not found. UI settings are disabled.");
            return null;
        }
        return provider.getProvider();
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
