package ru.realite.city;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

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
import ru.realite.city.i18n.CityMessages;

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
    private CityProtectionService protectionService;
    private SqliteCityAreaRepository cityAreaRepository;

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
        String lang = config.language(); // уже добавил поле language в CityConfig
        String fileName = (lang != null && lang.equalsIgnoreCase("en")) ? "messages_en.yml" : "messages_ru.yml";

        Config msgCfg = ctx.configs().loadOrCreateDefault(
                dataFolder.resolve(fileName), // на диске: рядом в папке плагина
                "lang/" + fileName, // в jar: resources/lang/...
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
        // сервисы
        selectionService = new CityAreaSelectionService();

        cityAreaRepository = new SqliteCityAreaRepository(storage);

        try {
            cityAreaRepository.loadAll();
        } catch (Exception e) {
            ctx.logger().error("[CityInfrastructure] Failed to load city areas", e);
        }

        protectionService = new CityProtectionService(config, cityAreaRepository);

        // Bukkit plugin instance (должно совпадать с name в plugin.yml)
        Plugin plugin = Bukkit.getPluginManager().getPlugin("RealiteCityInfrastructure");
        if (plugin == null) {
            ctx.logger()
                    .warn("[CityInfrastructure] Plugin RealiteCityInfrastructure not found; listeners not registered.");
            return;
        }

        // ✅ ВОТ ТУТ И ЕСТЬ РЕГИСТРАЦИЯ LISTENER'ОВ
        Bukkit.getPluginManager().registerEvents(
                new CityAreaSelectionListener(selectionService, messages),
                plugin);
        Bukkit.getPluginManager().registerEvents(
                new CityProtectionListener(protectionService, messages),
                plugin);

        ctx.logger().info("[CityInfrastructure] CityInfrastructure enabled");
       // До лучших времён ctx.logger().info("[CityInfrastructure] Loaded " + loaded + " city areas");
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
