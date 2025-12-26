package ru.realite.classes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.classes.command.ClassCommand;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.listener.ClassActionXpListener;
import ru.realite.classes.listener.MenuListener;
import ru.realite.classes.listener.PlayerJoinListener;
import ru.realite.classes.listener.PlayerQuitListener;
import ru.realite.classes.service.ClassHudService;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EffectService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.service.ProgressionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.storage.YamlProfileRepository;
import ru.realite.classes.util.Messages;
import java.io.File;
import java.io.InputStream;

public final class RealiteClassesPlugin extends JavaPlugin {

    private ClassService classService;

    // держим как поля, чтобы reload работал
    private Messages messages;
    private ClassConfigRepository classConfig;
    private XpConfigRepository xpConfig;
    private ClassSelectMenu menu;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        // ===== ресурсы и конфиги =====
        saveDefaultConfig();

        saveIfNotExists("classes.yml");
        saveIfNotExists("xp.yml");

        // локализации
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        // ===== ASCII + старт =====
        getLogger().info("");
        getLogger().info("§b  ____            _ _ _        _____ _                          ");
        getLogger().info("§b |  _ \\ ___  __ _| (_) |_ ___ | ____| | __ _ ___ ___  ___  ___ ");
        getLogger().info("§b | |_) / _ \\/ _` | | | __/ _ \\|  _| | |/ _` / __/ __|/ _ \\/ __|");
        getLogger().info("§b |  _ <  __/ (_| | | | ||  __/| |___| | (_| \\__ \\__ \\  __/\\__ \\");
        getLogger().info("§b |_| \\_\\___|\\__,_|_|_|\\__\\___||_____|_|\\__,_|___/___/\\___||___/");
        getLogger().info("§7 RealiteClasses v" + getDescription().getVersion());
        getLogger().info("");

        // ===== Vault check =====
        var vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null || !vault.isEnabled()) {
            getLogger().warning("Vault не найден или выключен!");
            getLogger().warning("Экономические функции будут недоступны.");
            getLogger().warning("Установи Vault + экономику (EssentialsX Economy, CMI, etc).");
        } else {
            getLogger().info("Vault найден: §a" + vault.getDescription().getVersion());
        }

        // ===== репозитории =====
        YamlProfileRepository profileRepo = new YamlProfileRepository(getDataFolder());

        this.classConfig = new ClassConfigRepository(getDataFolder());
        this.classConfig.reload();

        String lang = getConfig().getString("lang", "ru");
        this.messages = new Messages(this, lang);

        String changePerm = getConfig().getString(
                "permissions.change-class",
                "realiteclass.change");

        // ===== сервисы =====
        EvolutionService evolutionService = new EvolutionService(classConfig, changePerm);
        this.classService = new ClassService(profileRepo, evolutionService);

        EconomyService economy = new EconomyService(this); // сам решит, есть Vault или нет

        ProgressionService progressionService = new ProgressionService(
                classService,
                classConfig,
                evolutionService,
                messages);

        this.xpConfig = new XpConfigRepository(getDataFolder(), getLogger());

        ClassHudService hudService = new ClassHudService(
                classService,
                classConfig,
                evolutionService);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (var p : Bukkit.getOnlinePlayers()) {
                hudService.tick(p);
            }
        }, 20L, 20L);

        // ===== эффекты =====
        int tickSeconds = getConfig().getInt("effects.tick-seconds", 5);
        boolean clearManaged = getConfig().getBoolean("effects.clear-managed-effects", true);

        EffectService effectService = new EffectService(
                classService,
                classConfig,
                clearManaged);

        getServer().getScheduler().runTaskTimer(
                this,
                () -> getServer().getOnlinePlayers().forEach(effectService::applyFor),
                20L,
                tickSeconds * 20L);

        // ===== GUI =====
        this.menu = new ClassSelectMenu(this, classConfig);

        // ===== команды =====
        if (getCommand("class") != null) {
            getCommand("class").setExecutor(new ClassCommand(
                    this,
                    classService,
                    evolutionService,
                    classConfig,
                    economy,
                    messages,
                    xpConfig));
        } else {
            getLogger().severe("Command /class is not defined in plugin.yml!");
        }

        // ===== листенеры =====
        getServer().getPluginManager().registerEvents(
                new MenuListener(classService, classConfig, evolutionService, messages, hudService),
                this);

        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(classService),
                this);

        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(classService, hudService),
                this);

        getServer().getPluginManager().registerEvents(
                new ClassActionXpListener(classService, progressionService, xpConfig),
                this);

        // ===== автосейв =====
        int autosaveMinutes = getConfig().getInt("storage.autosave-minutes", 5);
        if (autosaveMinutes > 0) {
            long period = autosaveMinutes * 60L * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    classService.saveAll();
                } catch (Exception e) {
                    getLogger().log(java.util.logging.Level.SEVERE, "AutoSave failed", e);
                }
            }, period, period);
        }

        long took = System.currentTimeMillis() - start;
        getLogger().info("§aRealiteClasses успешно запущен. §7(" + took + "ms)");
        getLogger().info("");
    }

    public void reloadAll() {
        /**
         * Перезагрузка всех конфигов (messages/classes/xp + config.yml).
         * Важно: menu пересоздаем, чтобы обновились иконки/лоры.
         */
        reloadConfig();

        if (classConfig != null)
            classConfig.reload();
        if (messages != null)
            messages.reload(getConfig().getString("lang", "ru"));
        if (xpConfig != null)
            xpConfig.reload();

        // пересоздаем меню, потому что оно строится на classConfig
        this.menu = new ClassSelectMenu(this, classConfig);
    }

    public Messages getMessages() {
        return messages;
    }

    public ClassConfigRepository getClassConfig() {
        return classConfig;
    }

    public XpConfigRepository getXpConfig() {
        return xpConfig;
    }

    public ClassSelectMenu getMenu() {
        return menu;
    }

    private void saveIfNotExists(String resourcePath) {
        try {
            File out = new File(getDataFolder(), resourcePath);
            if (out.exists())
                return;

            // если ресурса нет в JAR — не падаем
            try (InputStream in = getResource(resourcePath)) {
                if (in == null) {
                    getLogger().warning("Resource not found in jar: " + resourcePath);
                    return;
                }
            }

            saveResource(resourcePath, false);
        } catch (Exception e) {
            getLogger().warning("Failed to save resource: " + resourcePath + " (" + e.getMessage() + ")");
        }
    }

    @Override
    public void onDisable() {
        if (classService != null) {
            classService.saveAll();
        }
        getLogger().info("RealiteClasses disabled.");
    }
}
