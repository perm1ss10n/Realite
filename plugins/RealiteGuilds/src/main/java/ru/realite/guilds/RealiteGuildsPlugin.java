package ru.realite.guilds;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.city.service.GuildsApi;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.integrations.CityAccessHook;
import ru.realite.core.api.guilds.GuildChatBridge;
import ru.realite.core.api.guilds.GuildTagProvider;
import ru.realite.core.api.logging.Banners;
import ru.realite.core.api.quests.GuildAdapter;
import ru.realite.guilds.command.GuildCommand;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.integration.CityGuildsApiAdapter;
import ru.realite.guilds.integration.GuildQuestAdapter;
import ru.realite.guilds.integration.NoopCityAccessHook;
import ru.realite.guilds.listener.GuildAccessProtectionListener;
import ru.realite.guilds.listener.GuildHomeWarmupListener;
import ru.realite.guilds.listener.GuildPveDamageListener;
import ru.realite.guilds.listener.GuildSalaryJoinListener;
import ru.realite.guilds.service.EconomyService;
import ru.realite.guilds.service.GuildChatBridgeImpl;
import ru.realite.guilds.service.GuildChatService;
import ru.realite.guilds.service.GuildChatTagProvider;
import ru.realite.guilds.service.GuildProgressionService;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.service.GuildSalaryService;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.service.GuildTreasuryService;
import ru.realite.guilds.service.GuildUpgradeEffectService;
import ru.realite.guilds.service.GuildUpgradeService;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

public final class RealiteGuildsPlugin extends JavaPlugin {

    private GuildMessages messages;
    private GuildRepository repository;
    private GuildService service;
    private GuildRankService rankService;
    private GuildSalaryService salaryService;
    private GuildChatService chatService;
    private GuildProgressionService progressionService;
    private GuildUpgradeConfigRepository upgradeConfigRepository;
    private GuildTreasuryService treasuryService;
    private GuildUpgradeService upgradeService;
    private GuildUpgradeEffectService upgradeEffectService;

    @Override
    public void onEnable() {
        // resources / config
        saveDefaultConfig();
        saveIfNotExists("ranks.yml");
        saveIfNotExists("upgrades.yml");
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        Banners.REALITE_GUILDS(this);

        // init
        messages = new GuildMessages(this);
        repository = new GuildRepository(this);
        rankService = new GuildRankService(this);

        EconomyService economyService = new EconomyService(this);

        upgradeConfigRepository = new GuildUpgradeConfigRepository(this);
        treasuryService = new GuildTreasuryService(this);
        upgradeEffectService = new GuildUpgradeEffectService(getConfig(), repository, upgradeConfigRepository);

        service = new GuildService(this, getConfig(), repository, messages, rankService, upgradeEffectService);
        salaryService = new GuildSalaryService(this, getConfig(), repository, messages, rankService, economyService);
        chatService = new GuildChatService(this, getConfig(), repository, messages, rankService);
        progressionService = new GuildProgressionService(this, getConfig(), repository, messages);

        upgradeService = new GuildUpgradeService(
                this,
                repository,
                rankService,
                upgradeConfigRepository,
                treasuryService,
                this::resolveCoreApi
        );

        // command
        PluginCommand command = getCommand("g");
        if (command != null) {
            command.setExecutor(new GuildCommand(
                    service,
                    messages,
                    salaryService,
                    chatService,
                    progressionService,
                    upgradeService
            ));
        } else {
            getLogger().severe("Command /g not found in plugin.yml; executor not registered.");
        }

        // listeners
        getServer().getPluginManager().registerEvents(new GuildHomeWarmupListener(service), this);

        CityAccessHook cityAccessHook = resolveCityAccessHook();
        getServer().getPluginManager().registerEvents(
                new GuildAccessProtectionListener(service, messages, getConfig(), cityAccessHook),
                this
        );

        getServer().getPluginManager().registerEvents(new GuildSalaryJoinListener(salaryService), this);
        getServer().getPluginManager().registerEvents(
                new GuildPveDamageListener(repository, service, upgradeEffectService),
                this
        );

        // services / adapters
        registerGuildChatBridge();
        registerGuildTagProvider();
        registerGuildQuestAdapter();
        registerCityGuildsApi();
    }

    @Override
    public void onDisable() {
        // важно для /reload и перезапусков: чтобы не оставлять старые провайдеры
        Bukkit.getServicesManager().unregisterAll(this);
    }

    private void registerGuildChatBridge() {
        Bukkit.getServicesManager().register(
                GuildChatBridge.class,
                new GuildChatBridgeImpl(chatService),
                this,
                ServicePriority.Normal
        );
    }

    private void registerGuildTagProvider() {
        withCore(core -> core.services().registerIfAbsent(
                GuildTagProvider.class,
                new GuildChatTagProvider(repository, messages, rankService)
        ));
    }

    private void registerGuildQuestAdapter() {
        withCore(core -> core.services().registerIfAbsent(
                GuildAdapter.class,
                new GuildQuestAdapter(repository, rankService)
        ));
    }

    private void registerCityGuildsApi() {
        getServer().getServicesManager().register(
                GuildsApi.class,
                new CityGuildsApiAdapter(repository),
                this,
                ServicePriority.Normal
        );
    }

    private CoreApi resolveCoreApi() {
        RegisteredServiceProvider<CoreApi> provider = getServer().getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
    }

    private void withCore(Consumer<CoreApi> action) {
        CoreApi core = resolveCoreApi();
        if (core != null) {
            action.accept(core);
        }
    }

    private CityAccessHook resolveCityAccessHook() {
        RegisteredServiceProvider<CityAccessHook> provider =
                getServer().getServicesManager().getRegistration(CityAccessHook.class);

        if (provider == null) {
            return new NoopCityAccessHook();
        }

        CityAccessHook hook = provider.getProvider();
        return hook == null ? new NoopCityAccessHook() : hook;
    }

    private void saveIfNotExists(String resourcePath) {
        try {
            File out = new File(getDataFolder(), resourcePath);
            if (out.exists()) {
                return;
            }

            File parent = out.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                getLogger().warning("Failed to create folders for: " + out);
                return;
            }

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
