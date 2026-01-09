package ru.realite.magic;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.classes.CoreClassesBridge;
import ru.realite.magic.integration.classes.NoopClassesBridge;
import ru.realite.magic.integration.events.BukkitEventPublisher;
import ru.realite.magic.integration.events.CoreEventPublisher;
import ru.realite.magic.integration.events.MagicEventPublisher;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.ItemsBridgeFactory;
import ru.realite.magic.listener.CombatListener;
import ru.realite.magic.listener.MagicInteractListener;
import ru.realite.magic.listener.MagicMenuListener;
import ru.realite.magic.listener.PlayerCleanupListener;
import ru.realite.magic.listener.SpellUnlockListener;
import ru.realite.magic.command.MagicCommand;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.PlayerSpellServiceImpl;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.storage.PlayerSpellStorage;
import ru.realite.magic.storage.YamlPlayerSpellStorage;

public final class RealiteMagicPlugin extends JavaPlugin {

    private MagicMessages messages;
    private MagicService magicService;
    private SpellRegistry spellRegistry;
    private PlayerSpellService playerSpellService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("spells/warlock_basic.yml");
        messages = new MagicMessages(this);
        spellRegistry = new SpellRegistry(this);
        var report = spellRegistry.load();
        if (report.hasErrors()) {
            getLogger().warning(messages.raw("magic.cmd.spells.errors.header"));
            for (var error : report.errors()) {
                getLogger().warning(messages.raw("magic.cmd.spells.errors.entry",
                        "file", error.fileName(),
                        "error", error.messageKey() == null
                                ? (error.message() == null ? messages.raw("magic.cmd.spells.errors.unknown") : error.message())
                                : messages.raw(error.messageKey(), error.placeholders())));
            }
        }
        PlayerSpellStorage storage = new YamlPlayerSpellStorage(this, messages);
        MagicEventPublisher eventPublisher = resolveEventPublisher();
        playerSpellService = new PlayerSpellServiceImpl(storage, spellRegistry, messages, eventPublisher);
        ItemsBridge itemsBridge = ItemsBridgeFactory.create();
        ClassesBridge classesBridge = resolveClassesBridge();
        magicService = new MagicService(this,
                messages,
                spellRegistry,
                playerSpellService,
                itemsBridge,
                classesBridge,
                eventPublisher);
        magicService.start();
        registerCommand();
        registerListeners();
    }

    public MagicMessages getMessages() {
        return messages;
    }

    public MagicService getMagicService() {
        return magicService;
    }

    public SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    public PlayerSpellService getPlayerSpellService() {
        return playerSpellService;
    }

    @Override
    public void onDisable() {
        if (magicService != null) {
            magicService.stop();
        }
        if (playerSpellService != null) {
            playerSpellService.flushAll();
        }
    }

    private void registerCommand() {
        var command = getCommand("rmagic");
        if (command == null) {
            getLogger().warning("Command /rmagic missing in plugin.yml");
            return;
        }
        MagicCommand magicCommand = new MagicCommand(magicService, playerSpellService, messages);
        command.setExecutor(magicCommand);
        command.setTabCompleter(magicCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(magicService), this);
        MagicInteractListener interactListener =
                new MagicInteractListener(magicService, messages);
        Bukkit.getPluginManager().registerEvents(
                new SpellUnlockListener(magicService, playerSpellService, messages), this);
        Bukkit.getPluginManager().registerEvents(
                new PlayerCleanupListener(magicService, playerSpellService), this);
        Bukkit.getPluginManager().registerEvents(
                new MagicMenuListener(magicService.spellSelectMenu(), spellRegistry, playerSpellService, messages), this);
        Bukkit.getPluginManager().registerEvents(interactListener, this);
    }

    private void saveIfNotExists(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + getDataFolder());
            return;
        }

        if (getResource(resourcePath) == null) {
            return;
        }

        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private ClassesBridge resolveClassesBridge() {
        CoreClassesBridge coreBridge = new CoreClassesBridge(getLogger());
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopClassesBridge();
    }

    private MagicEventPublisher resolveEventPublisher() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider != null && provider.getProvider() != null) {
            return new CoreEventPublisher(provider.getProvider().events());
        }
        return new BukkitEventPublisher();
    }
}
