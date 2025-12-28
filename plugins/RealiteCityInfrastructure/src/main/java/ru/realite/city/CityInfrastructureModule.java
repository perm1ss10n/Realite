package ru.realite.city;

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
        ctx.logger().info("[CityInfrastructure] CityInfrastructure enabled");
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
