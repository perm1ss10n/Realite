package ru.realite.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.module.ModuleManager;

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

    private CoreContext core;
    private ModuleManager modules;

    @Override
    public void onEnable() {
        // 1) Platform
        Platform platform = new PaperPlatform(this);

        // 2) Data folder
        saveDefaultConfig(); // если нет config.yml, не упадёт, просто создаст папку
        if (!getDataFolder().exists()) {
            // на всякий случай
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        // 3) Context + Services
        this.core = new CoreContext(this, platform);

        // Если используете Services как глобальный реестр — можно зарегистрировать базовые вещи:
        // (Если у тебя Services как я предлагал — с запретом перезаписи — это безопасно)
        try {
            Services.register(Platform.class, platform);
            Services.register(CoreContext.class, core);
        } catch (Exception e) {
            // если вдруг при /reload что-то осталось
            platform.warn("Services already had some entries. Clearing and re-registering...");
            Services.clear();
            Services.register(Platform.class, platform);
            Services.register(CoreContext.class, core);
        }

        platform.info("RealiteCore enabled.");

        // 4) Modules
        this.modules = new ModuleManager(core);

        // Пока регистрация ручная (чтобы не усложнять).
        // Позже сделаем автопоиск через ServiceLoader.
        registerBuiltinModules();

        // 5) Enable all modules
        modules.enableAll();
    }

    @Override
    public void onDisable() {
        if (modules != null) {
            modules.disableAll();
        }

        // чистим сервисы ядра (чтобы /reload не ловил мусор)
        Services.clear();

        if (core != null) {
            core.platform().info("RealiteCore disabled.");
        }
    }

    private void registerBuiltinModules() {
        // Сейчас тут пусто.
        // Пример как будет:
        //
        // modules.register(new ClassesModuleAdapter());
        // modules.register(new QuestsModuleAdapter());
        //
        // Где ClassesModuleAdapter — это тонкий класс, который вызывает onEnable у Classes-плагина/модуля.
    }

    public CoreContext core() {
        return core;
    }

    public ModuleManager modules() {
        return modules;
    }
}
