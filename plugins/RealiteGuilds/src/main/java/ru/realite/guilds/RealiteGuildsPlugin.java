package ru.realite.guilds;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.integrations.CityAccessHook;
import ru.realite.core.api.guilds.GuildTagProvider;
import ru.realite.guilds.integration.NoopCityAccessHook;
import ru.realite.guilds.command.GuildChatCommand;
import ru.realite.guilds.command.GuildCommand;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.listener.GuildAccessProtectionListener;
import ru.realite.guilds.listener.GuildChatListener;
import ru.realite.guilds.listener.GuildHomeWarmupListener;
import ru.realite.guilds.listener.GuildSalaryJoinListener;
import ru.realite.guilds.service.EconomyService;
import ru.realite.guilds.service.GuildChatService;
import ru.realite.guilds.service.GuildChatTagProvider;
import ru.realite.guilds.service.GuildProgressionService;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.service.GuildSalaryService;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.storage.GuildRepository;

public final class RealiteGuildsPlugin extends JavaPlugin {

    private GuildMessages messages;
    private GuildRepository repository;
    private GuildService service;
    private GuildRankService rankService;
    private GuildSalaryService salaryService;
    private GuildChatService chatService;
    private GuildProgressionService progressionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("ranks.yml", false);
        saveResource("lang/messages_ru.yml", false);
        saveResource("lang/messages_en.yml", false);

        messages = new GuildMessages(this);
        repository = new GuildRepository(this);
        rankService = new GuildRankService(this);
        EconomyService economyService = new EconomyService(this);
        service = new GuildService(this, getConfig(), repository, messages, rankService);
        salaryService = new GuildSalaryService(this, getConfig(), repository, messages, rankService, economyService);
        chatService = new GuildChatService(this, getConfig(), repository, messages, rankService);
        progressionService = new GuildProgressionService(this, getConfig(), repository, messages);

        PluginCommand command = getCommand("g");
        if (command != null) {
            command.setExecutor(new GuildCommand(service, messages, salaryService, chatService, progressionService));
        } else {
            getLogger().severe("Command 'g' not found in plugin.yml");
        }
        PluginCommand guildChatCommand = getCommand("gc");
        if (guildChatCommand != null) {
            guildChatCommand.setExecutor(new GuildChatCommand(chatService, messages));
        } else {
            getLogger().severe("Command 'gc' not found in plugin.yml");
        }

        getServer().getPluginManager().registerEvents(
                new GuildHomeWarmupListener(service), this);
        CityAccessHook cityAccessHook = resolveCityAccessHook();
        getServer().getPluginManager().registerEvents(
                new GuildAccessProtectionListener(service, messages, getConfig(), cityAccessHook), this);
        getServer().getPluginManager().registerEvents(
                new GuildSalaryJoinListener(salaryService), this);
        Plugin realiteChatPlugin = getServer().getPluginManager().getPlugin("RealiteChat");
        boolean realiteChatAvailable = realiteChatPlugin != null && realiteChatPlugin.isEnabled();
        getServer().getPluginManager().registerEvents(
                new GuildChatListener(chatService, realiteChatAvailable), this);
        registerGuildTagProvider();
    }

    private void registerGuildTagProvider() {
        RegisteredServiceProvider<CoreApi> provider = getServer().getServicesManager()
                .getRegistration(CoreApi.class);
        if (provider == null) {
            return;
        }
        CoreApi core = provider.getProvider();
        core.services().registerIfAbsent(GuildTagProvider.class, new GuildChatTagProvider(chatService));
    }

    private CityAccessHook resolveCityAccessHook() {
        RegisteredServiceProvider<CityAccessHook> provider = getServer().getServicesManager()
                .getRegistration(CityAccessHook.class);
        if (provider == null) {
            return new NoopCityAccessHook();
        }
        CityAccessHook hook = provider.getProvider();
        return hook == null ? new NoopCityAccessHook() : hook;
    }
}
