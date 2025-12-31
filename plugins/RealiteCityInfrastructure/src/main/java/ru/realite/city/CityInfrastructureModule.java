package ru.realite.city;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.city.command.CityCommand;
import ru.realite.city.gui.GuiService;
import ru.realite.city.gui.GuiSessionStore;
import ru.realite.city.gui.MenuFactory;
import ru.realite.city.gui.MenuListener;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.listener.ChatInputListener;
import ru.realite.city.listener.CityAreaSelectionListener;
import ru.realite.city.listener.CityProtectionListener;
import ru.realite.city.listener.ShopPointListener;
import ru.realite.city.service.ChatInputService;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.service.CityAdminService;
import ru.realite.city.service.CityInfrastructureAccessHook;
import ru.realite.city.service.CityHooks;
import ru.realite.city.service.DefaultCityHooks;
import ru.realite.city.service.EconomyService;
import ru.realite.city.service.GuildsApi;
import ru.realite.city.service.MarketService;
import ru.realite.city.service.NoopGuildsApi;
import ru.realite.city.service.PlotCleanupService;
import ru.realite.city.service.PlotService;
import ru.realite.city.service.ShopDirectoryService;
import ru.realite.city.service.ShopMarkerService;
import ru.realite.city.service.ShopPointService;
import ru.realite.city.service.ShopRentService;
import ru.realite.core.api.integrations.CityAccessHook;
import ru.realite.city.storage.SqliteCityAreaRepository;
import ru.realite.city.storage.SqlitePlotMemberRepository;
import ru.realite.city.storage.SqlitePlotRepository;
import ru.realite.city.storage.SqliteShopListingRepository;
import ru.realite.city.storage.SqliteShopPointRepository;
import ru.realite.core.api.Config;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class CityInfrastructureModule implements Module {

    private final ModuleMetadata metadata = new ModuleMetadata(
            new ModuleId("realite-city"),
            "RealiteCityInfrastructure",
            "0.1.0",
            Set.of());

    private Storage storage;
    private CityConfig config;
    private CityDatabase database;

    private CityMessages messages;

    private CityAreaSelectionService selectionService;
    private SqliteCityAreaRepository cityAreaRepository;

    private SqlitePlotRepository plotRepository;
    private SqlitePlotMemberRepository plotMemberRepository;
    private SqliteShopPointRepository shopPointRepository;
    private SqliteShopListingRepository shopListingRepository;
    private PlotService plotService;
    private PlotCleanupService plotCleanupService;
    private EconomyService economyService;
    private ShopPointService shopPointService;
    private ShopRentService shopRentService;
    private ShopMarkerService shopMarkerService;
    private ShopDirectoryService shopDirectoryService;
    private MarketService marketService;
    private CityHooks cityHooks;
    private GuildsApi guildsApi;
    private GuiService guiService;
    private ChatInputService chatInputService;

    @Override
    public ModuleMetadata metadata() {
        return metadata;
    }

    @Override
    public void onLoad(ModuleContext ctx) throws Exception {
        Path dataFolder = ctx.dataFolder();
        Files.createDirectories(dataFolder);

        Config cfg = ctx.configs().loadOrCreateDefault(
                dataFolder.resolve("config.yml"),
                "config.yml",
                getClass().getClassLoader());
        config = CityConfig.from(cfg);

        // Load localized messages (resources/lang/messages_*.yml)
        String lang = config.language();
        String fileName = (lang != null && lang.equalsIgnoreCase("en"))
                ? "messages_en.yml"
                : "messages_ru.yml";

        Config msgCfg = ctx.configs().loadOrCreateDefault(
                dataFolder.resolve(fileName), // disk: plugins/RealiteCityInfrastructure/...
                "lang/" + fileName, // jar: resources/lang/...
                getClass().getClassLoader());

        messages = new CityMessages(msgCfg);
        ctx.logger().info("[CityInfrastructure] Loaded language: " + (lang == null ? "ru" : lang));

        ctx.logger().info("[CityInfrastructure] Loaded config: defaultPlotsPerPlayer="
                + config.defaultPlotsPerPlayer()
                + ", dbFile=" + config.sqliteFile());

        storage = ctx.storage().openSqlite(dataFolder.resolve(config.sqliteFile()));
        database = new CityDatabase(storage);
        database.migrate();

        ctx.logger().info("[CityInfrastructure] Database migration OK");
        ctx.logger().info("[CityInfrastructure] CityInfrastructure loaded (db ready, config loaded)");
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        // Services
        selectionService = new CityAreaSelectionService();

        // Repositories
        cityAreaRepository = new SqliteCityAreaRepository(storage);
        plotRepository = new SqlitePlotRepository(storage);
        plotMemberRepository = new SqlitePlotMemberRepository(storage);
        shopPointRepository = new SqliteShopPointRepository(storage);
        shopListingRepository = new SqliteShopListingRepository(storage);

        // Optional warm-up / cache load (if your repos implement it)
        try {
            cityAreaRepository.loadAll();
            plotRepository.loadAll();
            plotMemberRepository.loadAll();
            shopPointRepository.loadAll();
            shopListingRepository.loadAll();
        } catch (Exception e) {
            ctx.logger().error("[CityInfrastructure] Failed to load city data", e);
        }

        // Bukkit plugin instance (must match name in plugin.yml)
        Plugin p = Bukkit.getPluginManager().getPlugin("RealiteCityInfrastructure");
        if (!(p instanceof JavaPlugin javaPlugin)) {
            ctx.logger().warn("[CityInfrastructure] Plugin RealiteCityInfrastructure not found or not JavaPlugin; "
                    + "commands/listeners were not registered.");
            return;
        }

        plotCleanupService = new PlotCleanupService(javaPlugin, config, messages);
        economyService = new EconomyService(javaPlugin);
        cityHooks = new DefaultCityHooks(config);
        guildsApi = resolveGuildsApi(javaPlugin);
        shopPointService = new ShopPointService(shopPointRepository);
        shopMarkerService = new ShopMarkerService(config, shopPointService);
        shopDirectoryService = new ShopDirectoryService(shopListingRepository, shopMarkerService);
        shopRentService = new ShopRentService(
                javaPlugin,
                config,
                messages,
                plotRepository,
                shopPointService,
                economyService);
        shopRentService.start();

        // Domain service
        plotService = new PlotService(
                config,
                cityAreaRepository,
                plotRepository,
                plotMemberRepository,
                economyService,
                cityHooks,
                guildsApi);
        marketService = new MarketService(config, economyService, cityHooks);
        chatInputService = new ChatInputService(javaPlugin, config, messages, plotService);
        CityAdminService adminService = new CityAdminService(selectionService, plotRepository);
        MenuFactory menuFactory = new MenuFactory(javaPlugin, messages, selectionService, plotRepository);
        GuiSessionStore guiSessionStore = new GuiSessionStore();
        guiService = new GuiService(
                config,
                messages,
                adminService,
                chatInputService,
                guiSessionStore,
                menuFactory,
                plotRepository,
                plotMemberRepository);
        javaPlugin.getServer().getServicesManager().register(
                CityAccessHook.class,
                new CityInfrastructureAccessHook(plotService),
                javaPlugin,
                ServicePriority.Normal);

        // Listeners
        Bukkit.getPluginManager().registerEvents(
                new CityAreaSelectionListener(selectionService, messages),
                javaPlugin);
        Bukkit.getPluginManager().registerEvents(
                new CityProtectionListener(plotService, messages, shopPointService),
                javaPlugin);
        Bukkit.getPluginManager().registerEvents(
                new ShopPointListener(shopPointService, plotRepository, plotMemberRepository, messages, shopRentService),
                javaPlugin);
        Bukkit.getPluginManager().registerEvents(
                new MenuListener(guiService, menuFactory),
                javaPlugin);
        Bukkit.getPluginManager().registerEvents(
                new ChatInputListener(javaPlugin, config, chatInputService),
                javaPlugin);

        // Command
        var cmd = javaPlugin.getCommand("city");
        if (cmd != null) {
            cmd.setExecutor(new CityCommand(
                    cityAreaRepository,
                    plotRepository,
                    plotMemberRepository,
                    plotService,
                    plotCleanupService,
                    selectionService,
                    messages,
                    config,
                    economyService,
                    shopPointService,
                    shopRentService,
                    shopDirectoryService,
                    shopMarkerService,
                    marketService,
                    guildsApi,
                    guiService));
        } else {
            ctx.logger().warn("[CityInfrastructure] Command /city not found in plugin.yml; executor not registered.");
        }

        var plotCmd = javaPlugin.getCommand("plot");
        if (plotCmd != null) {
            plotCmd.setExecutor(new ru.realite.city.command.PlotCommand(guiService, messages));
        } else {
            ctx.logger().warn("[CityInfrastructure] Command /plot not found in plugin.yml; executor not registered.");
        }

        ctx.logger().info("[CityInfrastructure] CityInfrastructure enabled");
    }

    private GuildsApi resolveGuildsApi(JavaPlugin plugin) {
        if (plugin == null) {
            return new NoopGuildsApi();
        }
        var provider = plugin.getServer().getServicesManager().getRegistration(GuildsApi.class);
        if (provider == null) {
            return new NoopGuildsApi();
        }
        GuildsApi api = provider.getProvider();
        return api == null ? new NoopGuildsApi() : api;
    }

    @Override
    public void onDisable(ModuleContext ctx) throws Exception {
        if (storage != null) {
            storage.close();
            storage = null;
        }
        ctx.logger().info("[CityInfrastructure] CityInfrastructure disabled");
    }
}
