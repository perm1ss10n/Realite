package ru.realite.magic;

import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.magic.api.MagicApi;
import ru.realite.magic.api.impl.MagicApiImpl;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.effect.DamageEffectExecutor;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.ParticlesEffectExecutor;
import ru.realite.magic.effect.PotionEffectExecutor;
import ru.realite.magic.effect.SoundEffectExecutor;
import ru.realite.magic.hud.MagicHudService;
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
import ru.realite.magic.listener.MasteryListener;
import ru.realite.magic.listener.PlayerCleanupListener;
import ru.realite.magic.listener.SpellBarListener;
import ru.realite.magic.listener.SpellUnlockListener;
import ru.realite.magic.mastery.MasteryService;
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
    private DebugService debugService;
    private MagicHudService hudService;
    private MagicApi magicApi;
    private MasteryService masteryService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("spells/warlock_basic.yml");
        messages = new MagicMessages(this);
        EffectExecutorRegistry effectRegistry = buildEffectRegistry();
        spellRegistry = new SpellRegistry(this, effectRegistry);
        var report = spellRegistry.load();
        if (report.hasErrors()) {
            getLogger().warning(messages.raw("magic.cmd.spells.errors.header"));
            for (var error : report.errors()) {
                if ("magic.cmd.spells.errors.schema".equals(error.messageKey())) {
                    getLogger().warning(messages.raw(error.messageKey(), error.placeholders()));
                    continue;
                }
                if (error.placeholders().containsKey("path")) {
                    String errorMessage = error.messageKey() == null
                            ? (error.message() == null ? messages.raw("magic.cmd.spells.errors.unknown") : error.message())
                            : messages.raw(error.messageKey(), error.placeholders());
                    getLogger().warning(messages.raw("magic.cmd.spells.errors.schema",
                            Map.of("file", error.fileName(),
                                    "path", error.placeholders().get("path"),
                                    "error", errorMessage)));
                    continue;
                }
                getLogger().warning(messages.raw("magic.cmd.spells.errors.entry",
                        "file", error.fileName(),
                        "error", error.messageKey() == null
                                ? (error.message() == null ? messages.raw("magic.cmd.spells.errors.unknown") : error.message())
                                : messages.raw(error.messageKey(), error.placeholders())));
            }
        }
        PlayerSpellStorage storage = new YamlPlayerSpellStorage(this, messages);
        MagicEventPublisher eventPublisher = resolveEventPublisher();
        PlayerSpellServiceImpl playerSpellServiceImpl =
                new PlayerSpellServiceImpl(storage, spellRegistry, messages, eventPublisher);
        playerSpellService = playerSpellServiceImpl;
        ItemsBridge itemsBridge = ItemsBridgeFactory.create();
        ClassesBridge classesBridge = resolveClassesBridge();
        debugService = new DebugService(messages);
        hudService = new MagicHudService(this, messages, spellRegistry);
        masteryService = new MasteryService(this, messages, spellRegistry, playerSpellServiceImpl);
        magicService = new MagicService(this,
                messages,
                spellRegistry,
                playerSpellService,
                itemsBridge,
                classesBridge,
                eventPublisher,
                effectRegistry,
                debugService,
                hudService,
                masteryService);
        magicService.start();
        magicApi = new MagicApiImpl(spellRegistry, playerSpellService, magicService);
        Bukkit.getServicesManager().register(MagicApi.class, magicApi, this, ServicePriority.Normal);
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
        if (magicApi != null) {
            Bukkit.getServicesManager().unregister(MagicApi.class, magicApi);
            magicApi = null;
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
        MagicCommand magicCommand = new MagicCommand(magicService, playerSpellService, messages, debugService, hudService);
        command.setExecutor(magicCommand);
        command.setTabCompleter(magicCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(magicService), this);
        MagicInteractListener interactListener =
                new MagicInteractListener(magicService);
        Bukkit.getPluginManager().registerEvents(
                new SpellUnlockListener(magicService, playerSpellService, messages), this);
        Bukkit.getPluginManager().registerEvents(
                new PlayerCleanupListener(magicService, playerSpellService), this);
        Bukkit.getPluginManager().registerEvents(
                new MagicMenuListener(magicService.spellSelectMenu(), spellRegistry, playerSpellService, messages), this);
        Bukkit.getPluginManager().registerEvents(interactListener, this);
        Bukkit.getPluginManager().registerEvents(
                new SpellBarListener(playerSpellService, hudService), this);
        Bukkit.getPluginManager().registerEvents(new MasteryListener(masteryService), this);
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

    private EffectExecutorRegistry buildEffectRegistry() {
        EffectExecutorRegistry registry = new EffectExecutorRegistry();
        registry.register(new DamageEffectExecutor());
        registry.register(new PotionEffectExecutor());
        registry.register(new ParticlesEffectExecutor());
        registry.register(new SoundEffectExecutor());
        return registry;
    }
}
