package ru.realite.core.impl;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.EventBus;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleManager;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.ModuleProvider;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Scheduler;
import ru.realite.core.api.Services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Главный плагин ядра.
 *
 * Сейчас делает:
 * - инициализирует Platform
 * - поднимает CoreContext
 * - включает ModuleManager
 *
 * Дальше сюда добавим загрузку модулей из classpath/ServiceLoader
 * или ручную регистрацию.
 */
public final class RealiteCorePlugin extends JavaPlugin {

    private CoreApi core;
    private ModuleManagerImpl modules;
    private Services services;

    @Override
    public void onEnable() {
        // 1) Platform
        Platform platform = new PaperPlatform(this);
        Scheduler scheduler = new BukkitSchedulerFacade(this);
        this.services = new ServicesImpl(scheduler);
        EventBus eventBus = new SimpleEventBus(platform);

        // 2) Data folder
        saveDefaultConfig(); // если нет config.yml, не упадёт, просто создаст папку
        if (!getDataFolder().exists()) {
            // на всякий случай
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // 3) Context + Services
        this.core = new CoreContext(this, platform, services, eventBus);

        // Если используете Services как глобальный реестр — можно зарегистрировать базовые вещи:
        // (Если у тебя Services как я предлагал — с запретом перезаписи — это безопасно)
        try {
            services.register(Platform.class, platform);
            services.register(CoreApi.class, core);
        } catch (Exception e) {
            // если вдруг при /reload что-то осталось
            platform.warn("Services already had some entries. Clearing and re-registering...");
            services.clear();
            services.register(Platform.class, platform);
            services.register(CoreApi.class, core);
        }

        getServer().getServicesManager()
                .register(CoreApi.class, core, this, ServicePriority.Normal);

        platform.info("RealiteCore enabled.");

        // 4) Modules
        this.modules = new ModuleManagerImpl(core);

        discoverModules(platform);
        getServer().getPluginManager().registerEvents(
                new ServerLoadModuleEnableListener(modules, core.platform()),
                this
        );
    }

    @Override
    public void onDisable() {
        if (modules != null) {
            modules.disableAll();
        }

        // чистим сервисы ядра (чтобы /reload не ловил мусор)
        if (services != null) {
            services.clear();
        }

        if (core != null) {
            core.platform().info("RealiteCore disabled.");
        }
    }

    private void discoverModules(Platform platform) {
        List<ModuleProvider> providers = loadProviders(platform);
        logProviders(platform, providers);

        for (ModuleProvider provider : providers) {
            Collection<Module> providedModules;
            try {
                providedModules = provider.createModules(core);
            } catch (Exception e) {
                platform.error("Failed to create modules from provider " + provider.getClass().getName(), e);
                continue;
            }

            if (providedModules == null || providedModules.isEmpty()) {
                platform.warn("ModuleProvider returned no modules: " + provider.getClass().getName());
                continue;
            }

            for (Module module : providedModules) {
                if (module == null) {
                    platform.warn("ModuleProvider returned null module: " + provider.getClass().getName());
                    continue;
                }
                try {
                    modules.register(module);
                } catch (Exception e) {
                    ModuleMetadata metadata = safeMetadata(module, platform);
                    if (metadata != null) {
                        modules.registerFailed(metadata, "Failed to register module", e);
                    } else {
                        platform.error("Failed to register module from provider " + provider.getClass().getName(), e);
                    }
                }
            }
        }

        logRegisteredModules(platform);
    }

    private List<ModuleProvider> loadProviders(Platform platform) {
        ServiceLoader<ModuleProvider> loader = ServiceLoader.load(ModuleProvider.class);
        List<ModuleProvider> providers = new ArrayList<>();
        var iterator = loader.iterator();
        while (iterator.hasNext()) {
            try {
                providers.add(iterator.next());
            } catch (ServiceConfigurationError e) {
                platform.error("Failed to load ModuleProvider", e);
            }
        }
        return providers;
    }

    private void logProviders(Platform platform, List<ModuleProvider> providers) {
        if (providers.isEmpty()) {
            platform.warn("No ModuleProvider found via ServiceLoader.");
            return;
        }
        platform.info("ModuleProviders found: " + providers.size());
        for (ModuleProvider provider : providers) {
            platform.info(" - " + provider.getClass().getName());
        }
    }

    private void logRegisteredModules(Platform platform) {
        Collection<Module> registered = modules.modules();
        platform.info("Registered modules: " + registered.size());
        for (Module module : registered) {
            ModuleMetadata metadata = module.metadata();
            String deps = metadata.dependencies().isEmpty()
                    ? "none"
                    : String.join(", ", metadata.dependencies().stream().map(ModuleId::value).toList());
            platform.info(" - " + metadata.id().value() + " (deps: " + deps + ")");
        }
    }

    private ModuleMetadata safeMetadata(Module module, Platform platform) {
        try {
            return module.metadata();
        } catch (Exception e) {
            platform.error("Failed to read module metadata for " + module.getClass().getName(), e);
            return null;
        }
    }

    public CoreApi core() {
        return core;
    }

    public ModuleManager modules() {
        return modules;
    }
}
