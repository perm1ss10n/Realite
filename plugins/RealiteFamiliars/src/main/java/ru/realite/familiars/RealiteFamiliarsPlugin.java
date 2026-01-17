package ru.realite.familiars;

import org.bukkit.command.PluginCommand;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Platform;
import ru.realite.familiars.command.FamiliarCommand;
import ru.realite.familiars.command.FamiliarsCommand;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.config.MessagesRepository;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.core.CoreAccess;
import ru.realite.familiars.listener.FamiliarSummonListener;
import ru.realite.familiars.listener.FamiliarTamingListener;
import ru.realite.familiars.service.FamiliarRepository;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.FamiliarServiceImpl;
import ru.realite.familiars.service.FamiliarStore;
import ru.realite.familiars.service.YamlFamiliarRepository;
import ru.realite.items.service.ItemService;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

public final class RealiteFamiliarsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private CoreApi core;
    private Platform platform;

    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private Messages messages;
    private FamiliarRepository repository;

    private FamiliarServiceImpl service;
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
        saveIfNotExists("messages.yml");

        reloadConfigs();

        if (service == null) {
            FamiliarStore store = new FamiliarStore();
            repository = new YamlFamiliarRepository(this, new File(getDataFolder(), "familiars-store.yml"));
            store.loadAll(repository.load());
            service = new FamiliarServiceImpl(core, this, store, repository, getLogger());
        }
        service.updateRepositories(typeRepository, rulesRepository);
        service.resetSummonedStates();
        core.services().replace(FamiliarService.class, service);

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
        if (service != null) {
            service.shutdown();
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
        messages = MessagesRepository.load(new File(dataFolder, "messages.yml"), getLogger())
                .map(MessagesRepository::messages)
                .orElseGet(this::defaultMessages);
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
        familiarCommand.setExecutor(new FamiliarCommand(service, messages));
    }

    private void registerListeners() {
        ItemService itemService = Bukkit.getServicesManager().load(ItemService.class);
        Bukkit.getPluginManager().registerEvents(
                new FamiliarTamingListener(service, messages, itemService, getLogger()),
                this
        );
        Bukkit.getPluginManager().registerEvents(new FamiliarSummonListener(service), this);
    }

    private Messages defaultMessages() {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        config.set("prefix", "<gray>[Familiars]</gray> ");
        config.set("debug.usage", "{prefix}<yellow>/familiars debug <typeId></yellow>");
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
        config.set("familiar.usage", "{prefix}<yellow>/familiar <summon|dismiss|follow|stay> [typeId]</yellow>");
        config.set("familiar.no-service", "{prefix}<red>Familiar service is not available.</red>");
        config.set("familiar.no-familiars", "{prefix}<red>You have no tamed familiars.</red>");
        config.set("familiar.specify-type", "{prefix}<yellow>Specify familiar type: {types}</yellow>");
        config.set("familiar.summon.success", "{prefix}<green>Familiar summoned: <type></green>");
        config.set("familiar.summon.failure", "{prefix}<red>Failed to summon familiar.</red>");
        config.set("familiar.dismiss.success", "{prefix}<green>Familiar dismissed: <type></green>");
        config.set("familiar.dismiss.failure", "{prefix}<red>Failed to dismiss familiar.</red>");
        config.set("familiar.behavior.follow", "{prefix}<green>Familiar set to follow: <type></green>");
        config.set("familiar.behavior.stay", "{prefix}<green>Familiar set to stay: <type></green>");
        config.set("familiar.behavior.failure", "{prefix}<red>Failed to update behavior.</red>");
        config.set("familiar.reasons", "{prefix}<red>Reasons:</red>");
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
