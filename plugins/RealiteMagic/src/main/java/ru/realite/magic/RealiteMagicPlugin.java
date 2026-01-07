package ru.realite.magic;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.listener.CombatListener;
import ru.realite.magic.listener.MagicInteractListener;
import ru.realite.magic.listener.MagicMenuListener;
import ru.realite.magic.listener.PlayerCleanupListener;
import ru.realite.magic.command.MagicCommand;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellRegistry;

public final class RealiteMagicPlugin extends JavaPlugin {

    private MagicMessages messages;
    private MagicService magicService;
    private SpellRegistry spellRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("spells/warlock_basic.yml");
        messages = new MagicMessages(this);
        spellRegistry = new SpellRegistry(this);
        spellRegistry.load();
        magicService = new MagicService(this, messages, spellRegistry);
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

    @Override
    public void onDisable() {
        if (magicService != null) {
            magicService.stop();
        }
    }

    private void registerCommand() {
        var command = getCommand("rmagic");
        if (command == null) {
            getLogger().warning("Command /rmagic missing in plugin.yml");
            return;
        }
        MagicCommand magicCommand = new MagicCommand(magicService, messages);
        command.setExecutor(magicCommand);
        command.setTabCompleter(magicCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(magicService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerCleanupListener(magicService), this);
        Bukkit.getPluginManager().registerEvents(new MagicMenuListener(magicService, messages), this);
        Bukkit.getPluginManager().registerEvents(new MagicInteractListener(magicService, messages), this);
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
}
