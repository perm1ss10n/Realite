package ru.realite.guilds;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.command.GuildCommand;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.storage.GuildRepository;

public final class RealiteGuildsPlugin extends JavaPlugin {

    private GuildMessages messages;
    private GuildRepository repository;
    private GuildService service;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("ranks.yml", false);
        saveResource("lang/messages_ru.yml", false);
        saveResource("lang/messages_en.yml", false);

        messages = new GuildMessages(this);
        repository = new GuildRepository(this);
        service = new GuildService(getConfig(), repository, messages);

        PluginCommand command = getCommand("g");
        if (command != null) {
            command.setExecutor(new GuildCommand(service, messages));
        } else {
            getLogger().severe("Command 'g' not found in plugin.yml");
        }
    }
}
