package ru.realite.city;

import ru.realite.core.api.Config;

public record CityConfig(
                int defaultPlotsPerPlayer,
                String sqliteFile,
                boolean debug,
                boolean cityAreaDefaultDeny,
                String cityAreaBypassPermission,
                boolean homePlotsEnabled,
                boolean shopPlotsEnabled,
                boolean plotCleanupEnabled,
                int plotCleanupBlocksPerTick,
                PlotCleanupMode plotCleanupMode,
                int plotNearbyDefaultRadius,
                String language) {
        public static CityConfig from(Config config) {
                int defaultPlotsPerPlayer = config.getInt("limits.defaultPlotsPerPlayer", 3);
                String sqliteFile = config.getString("storage.sqliteFile", "city.sqlite");
                boolean debug = config.getBoolean("logging.debug", false);
                boolean cityAreaDefaultDeny = config.getBoolean("cityArea.defaultDeny", true);
                String cityAreaBypassPermission = config.getString(
                                "cityArea.bypassPermission",
                                "realite.city.bypass");
                boolean homePlotsEnabled = config.getBoolean("plots.types.home.enabled", true);
                boolean shopPlotsEnabled = config.getBoolean("plots.types.shop.enabled", true);
                boolean plotCleanupEnabled = config.getBoolean("plots.cleanup.enabled", true);
                int plotCleanupBlocksPerTick = config.getInt("plots.cleanup.blocksPerTick", 2000);
                String cleanupModeRaw = config.getString("plots.cleanup.mode", "AIR_ONLY");
                PlotCleanupMode plotCleanupMode = PlotCleanupMode.fromToken(cleanupModeRaw);
                int plotNearbyDefaultRadius = config.getInt("plots.nearby.defaultRadius", 150);
                String language = config.getString("lang", config.getString("language", "ru"));

                return new CityConfig(
                                defaultPlotsPerPlayer,
                                sqliteFile,
                                debug,
                                cityAreaDefaultDeny,
                                cityAreaBypassPermission,
                                homePlotsEnabled,
                                shopPlotsEnabled,
                                plotCleanupEnabled,
                                plotCleanupBlocksPerTick,
                                plotCleanupMode,
                                plotNearbyDefaultRadius,
                                language);
        }
}
