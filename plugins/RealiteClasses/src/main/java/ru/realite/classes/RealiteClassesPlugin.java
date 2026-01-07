package ru.realite.classes;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import ru.realite.classes.command.ClassCommand;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.integration.ClassXpServiceAdapter;
import ru.realite.classes.listener.ClassActionXpListener;
import ru.realite.classes.listener.MenuListener;
import ru.realite.classes.listener.PlayerJoinListener;
import ru.realite.classes.listener.PlayerQuitListener;

import ru.realite.classes.service.ClassHudService;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.ClassTagProviderImpl;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EffectService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.service.EvolutionRequirementAdapter;
import ru.realite.classes.service.HiddenClassGate;
import ru.realite.classes.service.ProgressionService;

import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;
import ru.realite.classes.storage.TesterPresetRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.storage.YamlProfileRepository;

import ru.realite.classes.util.Messages;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Platform;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.core.api.classes.ClassXpService;
import ru.realite.core.api.logging.Banners;

import java.io.File;
import java.io.InputStream;

public final class RealiteClassesPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    // ===== Core (Core отдельным плагином) =====
    private CoreApi core;
    private Platform platform;

    // ===== configs / util =====
    private Messages messages;
    private ClassConfigRepository classConfig;
    private ClassLoreRepository classLore;
    private XpConfigRepository xpConfig;
    private TesterPresetRepository testerPresets;

    // ===== services =====
    private YamlProfileRepository profiles;
    private EconomyService economyService;
    private EvolutionService evolutionService;
    private ClassService classService;
    private ProgressionService progressionService;
    private EffectService effectService;
    private ClassHudService hudService;
    private ClassTagProvider classTagProvider;
    private ClassXpService classXpService;
    private EvolutionRequirementAdapter evolutionRequirementAdapter;
    private HiddenClassGate hiddenClassGate;

    // ===== gui =====
    private ClassSelectMenu menu;
    private final RealiteClassesEntrypoint entrypoint = new RealiteClassesEntrypoint(this);
    private boolean initialized;
    private boolean shuttingDown;

    @Override
    public void onEnable() {

        Banners.REALITE_CLASSES_WAITING(this);

        try {
            CoreApi c = ru.realite.classes.core.CoreAccess.core();
            if (c != null) {
                initialize(c);
            } else {
                getLogger().info("CoreApi not available yet. Waiting for module enable.");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize Classes plugin");
            e.printStackTrace();
        }
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
        saveIfNotExists("classes_lore.yml");
        saveIfNotExists("xp.yml");
        saveIfNotExists("tester-presets.yml");
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        // --- init ---
        reloadAll();

        // --- command ---
        var cmd = getCommand("class");
        if (cmd != null) {
            cmd.setExecutor(new ClassCommand(
                    this,
                    classService,
                    evolutionService,
                    classConfig,
                    economyService,
                    hiddenClassGate,
                    messages,
                    xpConfig));
        } else {
            getLogger().warning("Command /class not found in plugin.yml; executor not registered.");
        }

        // --- listeners ---
        Bukkit.getPluginManager().registerEvents(
                new MenuListener(classService, classConfig, evolutionService, hiddenClassGate, messages, hudService),
                this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(classService, effectService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(classService, hudService), this);
        Bukkit.getPluginManager().registerEvents(new ClassActionXpListener(classService, progressionService, xpConfig),
                this);

        initialized = true;
        platform.info("Enabled in " + (System.currentTimeMillis() - start) + "ms");
    }

    public void shutdownModule() {
        if (!initialized || shuttingDown) {
            return;
        }
        shuttingDown = true;
        if (classService != null) {
            classService.saveAll();
        }
        unregisterClassTagProvider();
        unregisterClassXpService();
        if (platform != null) {
            platform.info("Disabled");
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
        this.classLore = new ClassLoreRepository(data);
        this.xpConfig = new XpConfigRepository(data, getLogger());
        this.testerPresets = new TesterPresetRepository(data);

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

        this.evolutionRequirementAdapter = new EvolutionRequirementAdapter(classService);
        this.hiddenClassGate = new HiddenClassGate(
                classConfig,
                evolutionRequirementAdapter,
                () -> core.services().get(ru.realite.core.api.quests.QuestUnlockService.class));

        // --- menu ---
        this.menu = new ClassSelectMenu(this, classConfig, classLore, hiddenClassGate);

        registerClassTagProvider();
        registerClassXpService();

        platform.info("reloadAll completed");
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

    public HiddenClassGate getHiddenClassGate() {
        return hiddenClassGate;
    }

    public ClassConfigRepository getClassConfig() {
        return classConfig;
    }

    public XpConfigRepository getXpConfig() {
        return xpConfig;
    }

    public TesterPresetRepository getTesterPresets() {
        return testerPresets;
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

    public ClassHudService getHudService() {
        return hudService;
    }

    @Override
    public ru.realite.core.api.Module module() {
        return entrypoint.module();
    }

    // ===== utils =====

    private void saveIfNotExists(String resourcePath) {
        try {
            File out = new File(getDataFolder(), resourcePath);
            if (out.exists())
                return;

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

    private void registerClassTagProvider() {
        if (core == null || classService == null || evolutionService == null || classConfig == null) {
            return;
        }
        classTagProvider = new ClassTagProviderImpl(classService, evolutionService, classConfig);
        core.services().replace(ClassTagProvider.class, classTagProvider);
    }

    private void unregisterClassTagProvider() {
        if (core == null || classTagProvider == null) {
            return;
        }
        ClassTagProvider registered = core.services().get(ClassTagProvider.class);
        if (registered == classTagProvider) {
            core.services().unregister(ClassTagProvider.class);
        }
        classTagProvider = null;
    }

    private void registerClassXpService() {
        if (core == null || progressionService == null) {
            return;
        }
        classXpService = new ClassXpServiceAdapter(progressionService);
        core.services().replace(ClassXpService.class, classXpService);
    }

    private void unregisterClassXpService() {
        if (core == null || classXpService == null) {
            return;
        }
        ClassXpService registered = core.services().get(ClassXpService.class);
        if (registered == classXpService) {
            core.services().unregister(ClassXpService.class);
        }
        classXpService = null;
    }
}
