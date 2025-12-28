package ru.realite.core.impl;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.ModuleManager;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Services;

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
    private ModuleManager modules;
    private Services services;

    @Override
    public void onEnable() {
        // 1) Platform
        Platform platform = new PaperPlatform(this);
        this.services = new ServicesImpl();

        // 2) Data folder
        saveDefaultConfig(); // если нет config.yml, не упадёт, просто создаст папку
        if (!getDataFolder().exists()) {
            // на всякий случай
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // 3) Context + Services
        this.core = new CoreContext(this, platform, services);

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

        // Пока регистрация ручная (чтобы не усложнять).
        // Позже сделаем автопоиск через ServiceLoader.
        registerBuiltinModules();
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

    private void registerBuiltinModules() {
        modules.register(new BukkitPluginModuleAdapter(
                "classes",
                "RealiteClasses",
                java.util.List.of()
        ));
    }

    public CoreApi core() {
        return core;
    }

    public ModuleManager modules() {
        return modules;
    }
}
