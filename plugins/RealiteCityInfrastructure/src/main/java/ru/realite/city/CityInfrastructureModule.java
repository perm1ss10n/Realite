package ru.realite.city;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ru.realite.city.command.CityCommand;
import ru.realite.city.listener.CityAreaSelectionListener;
import ru.realite.city.listener.CityProtectionListener;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.service.CityProtectionService;
import ru.realite.city.storage.SqliteCityAreaRepository;
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
            Set.of()
    );

    private Storage storage;
    private CityConfig config;
    private CityDatabase database;
    private SqliteCityAreaRepository cityAreaRepository;
    private CityAreaSelectionService selectionService;
    private CityProtectionService protectionService;

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
                getClass().getClassLoader()
        );
        config = CityConfig.from(cfg);

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
        cityAreaRepository = new SqliteCityAreaRepository(storage);
        int loaded = 0;
        try {
            loaded = cityAreaRepository.loadAll();
        } catch (Exception e) {
            ctx.logger().error("[CityInfrastructure] Failed to load city areas", e);
        }

        selectionService = new CityAreaSelectionService();
        protectionService = new CityProtectionService(config, cityAreaRepository);

        Plugin plugin = Bukkit.getPluginManager().getPlugin("RealiteCityInfrastructure");
        if (plugin == null) {
            ctx.logger().warn("[CityInfrastructure] Plugin RealiteCityInfrastructure not found; "
                    + "commands and listeners were not registered.");
        } else {
            Bukkit.getPluginManager().registerEvents(
                    new CityAreaSelectionListener(selectionService),
                    plugin
            );
            Bukkit.getPluginManager().registerEvents(
                    new CityProtectionListener(protectionService),
                    plugin
            );
            if (plugin.getCommand("city") != null) {
                plugin.getCommand("city").setExecutor(
                        new CityCommand(cityAreaRepository, selectionService)
                );
            } else {
                ctx.logger().warn("[CityInfrastructure] Command 'city' is not defined in plugin.yml");
            }
        }

        ctx.logger().info("[CityInfrastructure] CityInfrastructure enabled");
        ctx.logger().info("[CityInfrastructure] Loaded " + loaded + " city areas");
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
