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

public final class RealiteClassesPlugin extends JavaPlugin {

    private ClassService classService;

    // держим как поля, чтобы reload работал
    private Messages messages;
    private ClassConfigRepository classConfig;
    private XpConfigRepository xpConfig;
    private ClassSelectMenu menu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("classes.yml", false);
        saveResource("messages.yml", false);
        saveResource("xp.yml", false);

        // репозитории
        YamlProfileRepository profileRepo = new YamlProfileRepository(getDataFolder());
        this.classConfig = new ClassConfigRepository(getDataFolder());
        this.classConfig.reload();

        // сообщения
        this.messages = new Messages(this);

        // permissions
        String changePerm = getConfig().getString("permissions.change-class", "realiteclass.change");

        // сервисы
        EvolutionService evolutionService = new EvolutionService(classConfig, changePerm);
        this.classService = new ClassService(profileRepo, evolutionService);

        EconomyService economy = new EconomyService(this);

        ProgressionService progressionService = new ProgressionService(classService, classConfig, evolutionService,
                messages);

        this.xpConfig = new XpConfigRepository(getDataFolder(), getLogger());

        ClassHudService hudService = new ClassHudService(classService, classConfig, evolutionService);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (var p : Bukkit.getOnlinePlayers()) {
                hudService.tick(p);
            }
        }, 20L, 20L); // раз в секунду

        // эффекты
        int tickSeconds = getConfig().getInt("effects.tick-seconds", 5);
        boolean clearManaged = getConfig().getBoolean("effects.clear-managed-effects", true);
        EffectService effectService = new EffectService(classService, classConfig, clearManaged);

        getServer().getScheduler().runTaskTimer(
                this,
                () -> getServer().getOnlinePlayers().forEach(effectService::applyFor),
                20L,
                tickSeconds * 20L);

        // GUI
        this.menu = new ClassSelectMenu(this, classConfig);

        // команды
        if (getCommand("class") != null) {
            getCommand("class").setExecutor(new ClassCommand(
                    this, // 👈 передаем плагин, чтобы дергать reload
                    classService,
                    evolutionService,
                    classConfig,
                    economy,
                    messages,
                    xpConfig));
        } else {
            getLogger().severe("Command /class is not defined in plugin.yml!");
        }

        // листенеры
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

        // автосейв
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

        getLogger().info("RealiteClasses enabled.");
    }

    /**
     * Перезагрузка всех конфигов (messages/classes/xp + config.yml).
     * Важно: menu пересоздаем, чтобы обновились иконки/лоры.
     */
    public void reloadAll() {
        reloadConfig();

        if (classConfig != null)
            classConfig.reload();
        if (messages != null)
            messages.reload();
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

    @Override
    public void onDisable() {
        if (classService != null) {
            classService.saveAll();
        }
        getLogger().info("RealiteClasses disabled.");
    }
}
