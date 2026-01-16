package ru.realite.familiars;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Platform;
import ru.realite.familiars.command.FamiliarsCommand;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.config.MessagesRepository;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.core.CoreAccess;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.FamiliarServiceImpl;
import ru.realite.familiars.service.FamiliarStore;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

public final class RealiteFamiliarsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private CoreApi core;
    private Platform platform;

    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private Messages messages;

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
            service = new FamiliarServiceImpl(core, new FamiliarStore());
        }
        service.updateRepositories(typeRepository, rulesRepository);
        core.services().replace(FamiliarService.class, service);

        registerCommand();

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
