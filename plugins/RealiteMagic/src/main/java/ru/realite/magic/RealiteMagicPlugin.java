package ru.realite.magic;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.classes.CoreClassesBridge;
import ru.realite.magic.integration.classes.NoopClassesBridge;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.ItemsBridgeFactory;
import ru.realite.magic.listener.CombatListener;
import ru.realite.magic.listener.MagicInteractListener;
import ru.realite.magic.listener.MagicMenuListener;
import ru.realite.magic.listener.PlayerCleanupListener;
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
        spellRegistry.load();
        PlayerSpellStorage storage = new YamlPlayerSpellStorage(this, messages);
        playerSpellService = new PlayerSpellServiceImpl(storage, spellRegistry, messages);
        ItemsBridge itemsBridge = ItemsBridgeFactory.create();
        ClassesBridge classesBridge = resolveClassesBridge();
        magicService = new MagicService(this, messages, spellRegistry, playerSpellService, itemsBridge, classesBridge);
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
                new MagicInteractListener(magicService, playerSpellService, spellRegistry, messages);
        Bukkit.getPluginManager().registerEvents(
                new PlayerCleanupListener(magicService, playerSpellService, interactListener), this);
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
}
