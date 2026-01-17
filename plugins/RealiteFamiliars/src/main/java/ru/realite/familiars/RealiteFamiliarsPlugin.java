package ru.realite.familiars;

import org.bukkit.command.PluginCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Subscription;
import ru.realite.core.api.classes.ClassProfileProvider;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.models.ModelsBridge;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.familiars.command.FamiliarCommand;
import ru.realite.familiars.command.FamiliarsCommand;
import ru.realite.familiars.config.FamiliarLimitsRepository;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.config.MessagesRepository;
import ru.realite.familiars.config.TamePolicyRepository;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.core.CoreAccess;
import ru.realite.familiars.integration.classes.ClassesBridge;
import ru.realite.familiars.integration.classes.CoreClassesBridge;
import ru.realite.familiars.integration.classes.NoopClassesBridge;
import ru.realite.familiars.integration.items.CoreItemsBridge;
import ru.realite.familiars.integration.items.ItemsBridge;
import ru.realite.familiars.integration.items.NoopItemsBridge;
import ru.realite.familiars.integration.limits.CityGuildBridge;
import ru.realite.familiars.integration.limits.NoopCityGuildBridge;
import ru.realite.familiars.integration.magic.MagicBridge;
import ru.realite.familiars.integration.magic.NoopMagicBridge;
import ru.realite.familiars.integration.models.NoopModelsBridge;
import ru.realite.familiars.integration.quests.CoreQuestsBridge;
import ru.realite.familiars.integration.quests.FamiliarQuestXpEvent;
import ru.realite.familiars.integration.quests.NoopQuestsBridge;
import ru.realite.familiars.integration.quests.QuestsBridge;
import ru.realite.familiars.listener.FamiliarCombatListener;
import ru.realite.familiars.listener.FamiliarInventoryListener;
import ru.realite.familiars.listener.FamiliarXpListener;
import ru.realite.familiars.listener.FamiliarSummonListener;
import ru.realite.familiars.listener.FamiliarTamingListener;
import ru.realite.familiars.service.FamiliarRepository;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.FamiliarServiceImpl;
import ru.realite.familiars.service.FamiliarStore;
import ru.realite.familiars.service.FamiliarXpSource;
import ru.realite.familiars.service.FamiliarLimitService;
import ru.realite.familiars.service.VirtualInventoryService;
import ru.realite.familiars.service.VirtualInventoryServiceImpl;
import ru.realite.familiars.service.YamlFamiliarRepository;
import ru.realite.familiars.ui.FamiliarActionBarService;
import ru.realite.familiars.ui.FamiliarUiServiceImpl;
import ru.realite.items.service.ItemService;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

public final class RealiteFamiliarsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private CoreApi core;
    private Platform platform;

    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private FamiliarLimitsRepository limitsRepository;
    private TamePolicyRepository policyRepository;
    private Messages messages;
    private FamiliarRepository repository;

    private FamiliarServiceImpl service;
    private FamiliarUiService uiService;
    private VirtualInventoryService virtualInventoryService;
    private FamiliarActionBarService actionBarService;
    private ItemsBridge itemsBridge;
    private ClassesBridge classesBridge;
    private QuestsBridge questsBridge;
    private MagicBridge magicBridge;
    private CityGuildBridge cityGuildBridge;
    private ModelsBridge modelsBridge;
    private FamiliarLimitService limitService;
    private Subscription questXpSubscription;
    private final RealiteFamiliarsEntrypoint entrypoint = new RealiteFamiliarsEntrypoint(this);
    private boolean initialized;
    private boolean shuttingDown;

    @Override
    public void onEnable() {
        try {
            CoreApi api = CoreAccess.core();
            initialize(api);
        } catch (Exception e) {
            getLogger().info("CoreApi not available yet. Waiting for module enable.");
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
        this.core = Objects.requireNonNull(core, "core");
        this.platform = core.platform();

        saveDefaultConfig();
        saveIfNotExists("familiars.yml");
        saveIfNotExists("taming.yml");
        saveIfNotExists("limits.yml");
        saveIfNotExists("tame-policy.yml");
        saveIfNotExists("messages.yml");

        reloadConfigs();

        classesBridge = resolveClassesBridge();
        itemsBridge = resolveItemsBridge();
        questsBridge = resolveQuestsBridge();
        magicBridge = resolveMagicBridge();
        cityGuildBridge = resolveCityGuildBridge();
        modelsBridge = resolveModelsBridge();
        limitService = new FamiliarLimitService(classesBridge, limitsRepository);

        if (service == null) {
            FamiliarStore store = new FamiliarStore();
            repository = new YamlFamiliarRepository(this, new File(getDataFolder(), "familiars-store.yml"));
            store.loadAll(repository.load());
            service = new FamiliarServiceImpl(this, store, repository, getLogger(),
                    classesBridge, questsBridge, magicBridge, cityGuildBridge, modelsBridge, limitService);
        }
        service.updateRepositories(typeRepository, rulesRepository, limitsRepository, policyRepository);
        service.resetSummonedStates();
        core.services().replace(FamiliarService.class, service);
        if (virtualInventoryService == null) {
            virtualInventoryService = new VirtualInventoryServiceImpl(service, itemsBridge);
        }
        if (uiService == null) {
            uiService = new FamiliarUiServiceImpl(service, virtualInventoryService);
        }
        core.services().replace(FamiliarUiService.class, uiService);
        actionBarService = new FamiliarActionBarService(messages);

        subscribeQuestXp();

        registerCommand();
        registerListeners();

        initialized = true;
        platform.info("RealiteFamiliars enabled");
    }

    public void shutdownModule() {
        if (!initialized || shuttingDown) {
            return;
        }
        shuttingDown = true;
        if (core != null && service != null) {
            FamiliarService registered = core.services().get(FamiliarService.class);
            if (registered == service) {
                core.services().unregister(FamiliarService.class);
            }
        }
        if (core != null && uiService != null) {
            FamiliarUiService registeredUi = core.services().get(FamiliarUiService.class);
            if (registeredUi == uiService) {
                core.services().unregister(FamiliarUiService.class);
            }
        }
        if (service != null) {
            service.shutdown();
        }
        if (questXpSubscription != null) {
            questXpSubscription.unsubscribe();
            questXpSubscription = null;
        }
        initialized = false;
        shuttingDown = false;
        core = null;
        platform = null;
    }

    public void reloadConfigs() {
        File dataFolder = getDataFolder();
        typeRepository = FamiliarTypeRepository.load(new File(dataFolder, "familiars.yml"), getLogger())
                .orElse(null);
        rulesRepository = TamingRulesRepository.load(new File(dataFolder, "taming.yml"), getLogger())
                .orElse(null);
        limitsRepository = FamiliarLimitsRepository.load(new File(dataFolder, "limits.yml"), getLogger())
                .orElse(null);
        policyRepository = TamePolicyRepository.load(new File(dataFolder, "tame-policy.yml"), getLogger())
                .orElse(null);
        messages = MessagesRepository.load(new File(dataFolder, "messages.yml"), getLogger())
                .map(MessagesRepository::messages)
                .orElseGet(this::defaultMessages);
        if (limitService != null) {
            limitService.updateRepository(limitsRepository);
        }
    }

    @Override
    public ru.realite.core.api.Module module() {
        return entrypoint.module();
    }

    private void registerCommand() {
        PluginCommand command = getCommand("familiars");
        if (command == null) {
            getLogger().warning("Command /familiars not found in plugin.yml; executor not registered.");
            return;
        }
        command.setExecutor(new FamiliarsCommand(service, messages));
        PluginCommand familiarCommand = getCommand("familiar");
        if (familiarCommand == null) {
            getLogger().warning("Command /familiar not found in plugin.yml; executor not registered.");
            return;
        }
        UiScreenRegistry screenRegistry = core.services().get(UiScreenRegistry.class);
        familiarCommand.setExecutor(new FamiliarCommand(service, messages, actionBarService, screenRegistry));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(
                new FamiliarTamingListener(service, messages, itemsBridge, getLogger(), actionBarService),
                this
        );
        Bukkit.getPluginManager().registerEvents(new FamiliarSummonListener(service), this);
        Bukkit.getPluginManager().registerEvents(new FamiliarXpListener(service), this);
        Bukkit.getPluginManager().registerEvents(new FamiliarCombatListener(service), this);
        Bukkit.getPluginManager().registerEvents(new FamiliarInventoryListener(virtualInventoryService), this);
    }

    private ClassesBridge resolveClassesBridge() {
        CoreClassesBridge coreBridge = new CoreClassesBridge(
                () -> core.services().get(ClassProfileProvider.class),
                () -> core.services().get(ClassTagProvider.class));
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopClassesBridge(getLogger());
    }

    private ItemsBridge resolveItemsBridge() {
        ItemService itemService = Bukkit.getServicesManager().load(ItemService.class);
        if (itemService != null) {
            return new CoreItemsBridge(itemService);
        }
        return new NoopItemsBridge(getLogger());
    }

    private QuestsBridge resolveQuestsBridge() {
        if (core != null) {
            return new CoreQuestsBridge(core.events());
        }
        return new NoopQuestsBridge(getLogger());
    }

    private MagicBridge resolveMagicBridge() {
        return new NoopMagicBridge(getLogger());
    }

    private CityGuildBridge resolveCityGuildBridge() {
        return new NoopCityGuildBridge(getLogger());
    }

    private ModelsBridge resolveModelsBridge() {
        if (core == null) {
            return new NoopModelsBridge(getLogger());
        }
        ModelsBridge bridge = core.services().get(ModelsBridge.class);
        if (bridge != null && bridge.isAvailable()) {
            return bridge;
        }
        return new NoopModelsBridge(getLogger());
    }

    private void subscribeQuestXp() {
        if (core == null || service == null) {
            return;
        }
        if (questXpSubscription != null) {
            questXpSubscription.unsubscribe();
        }
        questXpSubscription = core.events().subscribe(FamiliarQuestXpEvent.class, this::handleQuestXp);
    }

    private void handleQuestXp(FamiliarQuestXpEvent event) {
        if (event == null || service == null) {
            return;
        }
        service.addExperience(event.ownerId(), event.familiarTypeId(), event.amount(), FamiliarXpSource.QUEST);
    }

    private Messages defaultMessages() {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("prefix", "<gray>[Familiars]</gray> ");
        config.set("debug.usage", "{prefix}<yellow>/familiars debug <typeId|limits></yellow>");
        config.set("debug.limits", "{prefix}<yellow>Limit: <white>{limit}</white> <gray>({source})</gray></yellow>");
        config.set("debug.no-service", "{prefix}<red>Familiar service is not available.</red>");
        config.set("debug.header", "{prefix}<yellow>Debug for <type></yellow>");
        config.set("debug.can-tame", "{prefix}Can tame: {result}");
        config.set("debug.can-summon", "{prefix}Can summon: {result}");
        config.set("debug.reasons", "{prefix}<red>Reasons:</red>");
        config.set("debug.notes", "{prefix}<gray>Notes:</gray>");
        config.set("taming.success", "{prefix}<green>Familiar tamed: <type></green>");
        config.set("taming.failure", "{prefix}<red>Failed to tame familiar.</red>");
        config.set("taming.failure-reason", "{prefix}<red>Failed to tame: <reason></red>");
        config.set("taming.missing-type", "{prefix}<red>Tag does not specify a familiar type.</red>");
        config.set("familiar.usage", "{prefix}<yellow>/familiar <list|summon|dismiss|follow|stay|ui> [slot|typeId]</yellow>");
        config.set("familiar.no-service", "{prefix}<red>Familiar service is not available.</red>");
        config.set("familiar.no-familiars", "{prefix}<red>You have no tamed familiars.</red>");
        config.set("familiar.invalid-slot", "{prefix}<red>Invalid familiar slot.</red>");
        config.set("familiar.specify-type", "{prefix}<yellow>Specify familiar slot or type: {types}</yellow>");
        config.set("familiar.list.header", "{prefix}<yellow>Your familiars:</yellow>");
        config.set("familiar.list.entry", "{prefix}<gray>{slot}. <white>{type}</white> <gray>({state})</gray></gray>");
        config.set("familiar.list.state.tamed", "<yellow>tamed</yellow>");
        config.set("familiar.list.state.summoned", "<green>summoned</green>");
        config.set("familiar.summon.success", "{prefix}<green>Familiar summoned: <type></green>");
        config.set("familiar.summon.failure", "{prefix}<red>Failed to summon familiar.</red>");
        config.set("familiar.dismiss.success", "{prefix}<green>Familiar dismissed: <type></green>");
        config.set("familiar.dismiss.failure", "{prefix}<red>Failed to dismiss familiar.</red>");
        config.set("familiar.behavior.follow", "{prefix}<green>Familiar set to follow: <type></green>");
        config.set("familiar.behavior.stay", "{prefix}<green>Familiar set to stay: <type></green>");
        config.set("familiar.behavior.failure", "{prefix}<red>Failed to update behavior.</red>");
        config.set("familiar.reasons", "{prefix}<red>Reasons:</red>");
        config.set("familiar.menu.unavailable", "{prefix}<red>Familiar menu is unavailable.</red>");
        config.set("actionbar.tamed", "<green>приручено</green>");
        config.set("actionbar.class", "<red>нельзя: класс</red>");
        config.set("actionbar.policy", "<red>нельзя: моб</red>");
        config.set("actionbar.limit", "<red>лимит</red>");
        config.set("actionbar.cooldown", "<red>кд</red>");
        config.set("hud.actionbar", "<yellow>{type}</yellow> <gray>{role}</gray> <white>Lv {level}</white> "
                + "<green>{xp}%</green> <gray>{state}</gray> <gray>{distance}</gray> <red>{hp}</red>");
        config.set("hud.state.tamed", "<yellow>tamed</yellow>");
        config.set("hud.state.summoned", "<green>summoned</green>");
        config.set("menu.title", "<gold>Familiars</gold>");
        config.set("menu.empty", "<gray>No familiars.</gray>");
        config.set("menu.close", "<red>Close</red>");
        config.set("menu.action.summon", "<green>Summon</green>");
        config.set("menu.action.dismiss", "<red>Dismiss</red>");
        config.set("menu.action.follow", "<green>Follow</green>");
        config.set("menu.action.stay", "<yellow>Stay</yellow>");
        config.set("menu.action.rename", "<gray>Rename</gray>");
        config.set("menu.action.rename_soon", "<gray>Soon.</gray>");
        config.set("menu.action.back", "<yellow>Back</yellow>");
        config.set("menu.action.unavailable", "<dark_gray>Unavailable</dark_gray>");
        config.set("menu.familiar.name", "<white>{type}</white> <gray>Lv {level}</gray>");
        config.set("menu.familiar.title", "<gold>{type}</gold>");
        config.set("menu.familiar.missing", "<red>Familiar not found.</red>");
        config.set("menu.familiar.lore.role", "<gray>Role: <white>{role}</white></gray>");
        config.set("menu.familiar.lore.level", "<gray>Level: <white>{level}</white> <gray>XP: <white>{xp}%</white></gray>");
        config.set("menu.familiar.lore.state", "<gray>Status: <white>{state}</white></gray>");
        config.set("menu.state.tamed", "<yellow>tamed</yellow>");
        config.set("menu.state.summoned", "<green>summoned</green>");
        return new Messages(config);
    }

    private void saveIfNotExists(String resourcePath) {
        try {
            File out = new File(getDataFolder(), resourcePath);
            if (out.exists()) {
                return;
            }
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
