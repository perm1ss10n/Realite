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

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Platform;

import java.io.File;
import java.io.InputStream;

public final class RealiteClassesPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    // ===== Core (Core отдельным плагином) =====
    private CoreApi core;
    private Platform platform;

    // ===== configs / util =====
    private Messages messages;
    private ClassConfigRepository classConfig;
    private XpConfigRepository xpConfig;

    // ===== services =====
    private YamlProfileRepository profiles;
    private EconomyService economyService;
    private EvolutionService evolutionService;
    private ClassService classService;
    private ProgressionService progressionService;
    private EffectService effectService;
    private ClassHudService hudService;

    // ===== gui =====
    private ClassSelectMenu menu;
    private final RealiteClassesEntrypoint entrypoint = new RealiteClassesEntrypoint(this);
    private boolean initialized;
    private boolean shuttingDown;

    @Override
    public void onEnable() {
        getLogger().info("RealiteClasses loaded. Waiting for module enable.");
    }

    @Override
    public void onDisable() {
        shutdownModule();
    }

    public void initialize(CoreApi core) {
        if (initialized) {
            return;
        }
        this.shuttingDown = false;
        long start = System.currentTimeMillis();

        this.core = java.util.Objects.requireNonNull(core, "core");
        this.platform = core.platform();

        // --- resources ---
        saveDefaultConfig();
        saveIfNotExists("classes.yml");
        saveIfNotExists("xp.yml");
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        // --- init ---
        reloadAll();

        // --- command ---
        getCommand("class").setExecutor(
                new ClassCommand(
                        this,
                        classService,
                        evolutionService,
                        classConfig,
                        economyService,
                        messages,
                        xpConfig));

        // --- listeners ---
        Bukkit.getPluginManager().registerEvents(
                new MenuListener(classService, classConfig, evolutionService, messages, hudService),
                this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(classService, effectService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(classService, hudService), this);
        Bukkit.getPluginManager().registerEvents(new ClassActionXpListener(classService, progressionService, xpConfig),
                this);

        initialized = true;
        platform.info("[Classes] Enabled in " + (System.currentTimeMillis() - start) + "ms");
    }

    public void shutdownModule() {
        if (!initialized || shuttingDown) {
            return;
        }
        shuttingDown = true;
        if (classService != null) {
            classService.saveAll();
        }
        if (platform != null) {
            platform.info("[Classes] Disabled");
        }
        initialized = false;
        shuttingDown = false;
        core = null;
        platform = null;
    }

    /**
     * Полная перезагрузка всей логики классов
     */
    public void reloadAll() {
        reloadConfig();

        File data = getDataFolder();

        // --- messages ---
        String lang = getConfig().getString("lang", "ru");
        this.messages = new Messages(this, lang);

        // --- configs ---
        this.classConfig = new ClassConfigRepository(data);
        this.xpConfig = new XpConfigRepository(data, getLogger());

        // --- repositories ---
        this.profiles = new YamlProfileRepository(data);

        // --- economy ---
        this.economyService = new EconomyService(this);

        // --- evolution ---
        String changePermission = getConfig().getString("change-class-permission", "realite.classes.change");
        this.evolutionService = new EvolutionService(classConfig, changePermission);

        // --- class service ---
        this.classService = new ClassService(profiles, evolutionService);

        // --- progression ---
        this.progressionService = new ProgressionService(classService, classConfig, evolutionService, messages);

        // --- effects ---
        boolean clearManaged = getConfig().getBoolean("clear-managed-effects", true);
        this.effectService = new EffectService(classService, classConfig, clearManaged);

        // --- HUD ---
        this.hudService = new ClassHudService(classService, classConfig, evolutionService);

        // --- menu ---
        this.menu = new ClassSelectMenu(this, classConfig);

        platform.info("[Classes] reloadAll completed");
    }

    // ===== getters =====

    public CoreApi core() {
        return core;
    }

    public Platform platform() {
        return platform;
    }

    public ClassService getClassService() {
        return classService;
    }

    public Messages getMessages() {
        return messages;
    }

    public ClassSelectMenu getMenu() {
        return menu;
    }

    public ClassConfigRepository getClassConfig() {
        return classConfig;
    }

    public XpConfigRepository getXpConfig() {
        return xpConfig;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public EvolutionService getEvolutionService() {
        return evolutionService;
    }

    public ProgressionService getProgressionService() {
        return progressionService;
    }

    @Override
    public ru.realite.core.api.Module module() {
        return entrypoint.module();
    }

    // ===== utils =====

    private void saveIfNotExists(String resourcePath) {
        try {
            File out = new File(getDataFolder(), resourcePath);
            if (out.exists()) return;

            out.getParentFile().mkdirs();

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
}
