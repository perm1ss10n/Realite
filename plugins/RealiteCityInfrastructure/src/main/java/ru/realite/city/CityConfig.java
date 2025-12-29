package ru.realite.city;

import ru.realite.core.api.Config;

public record CityConfig(
                int defaultPlotsPerPlayer,
                int homePlotsPerPlayer,
                int shopPlotsPerPlayer,
                String sqliteFile,
                boolean debug,
                boolean cityAreaDefaultDeny,
                String cityAreaBypassPermission,
                boolean homePlotsEnabled,
                boolean shopPlotsEnabled,
                boolean plotCleanupEnabled,
                int plotCleanupBlocksPerTick,
                PlotCleanupMode plotCleanupMode,
                String plotCleanupFillBlock,
                int plotCleanupClearAboveY,
                int plotNearbyDefaultRadius,
                boolean allowInteractOutsideMembers,
                int shopPointsPerPlot,
                boolean shopRentEnabled,
                int shopRentPeriodHours,
                int shopRentPricePerPeriod,
                int shopRentGraceHours,
                int shopRentCheckMinutes,
                boolean shopMarkerEnabled,
                String shopMarkerFormatLine1,
                String shopMarkerFormatLine2,
                boolean shopMarkerUpdateOnChange,
                boolean marketTeleportEnabled,
                String marketTeleportPermission,
                int marketTeleportCooldownSeconds,
                double marketTeleportCost,
                int marketNearbyDefaultRadius,
                String marketHubWorld,
                double marketHubX,
                double marketHubY,
                double marketHubZ,
                String language) {
        public static CityConfig from(Config config) {
                int defaultPlotsPerPlayer = config.getInt("limits.defaultPlotsPerPlayer", 3);
                int homePlotsPerPlayer = config.getInt("limits.perType.home", defaultPlotsPerPlayer);
                int shopPlotsPerPlayer = config.getInt("limits.perType.shop", defaultPlotsPerPlayer);
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
                String plotCleanupFillBlock = config.getString("plots.cleanup.fillBlock", "GRASS_BLOCK");
                int plotCleanupClearAboveY = config.getInt("plots.cleanup.clearAboveY", -1);
                int plotNearbyDefaultRadius = config.getInt("plots.nearby.defaultRadius", 150);
                boolean allowInteractOutsideMembers =
                                config.getBoolean("plots.protection.allowInteractOutsideMembers", false);
                int shopPointsPerPlot = config.getInt("shops.pointsPerPlot", 3);
                boolean shopRentEnabled = config.getBoolean("shops.rent.enabled", false);
                int shopRentPeriodHours = config.getInt("shops.rent.periodHours", 24);
                int shopRentPricePerPeriod = config.getInt("shops.rent.pricePerPeriod", 100);
                int shopRentGraceHours = config.getInt("shops.rent.graceHours", 48);
                int shopRentCheckMinutes = config.getInt("shops.rent.checkMinutes", 10);
                boolean shopMarkerEnabled = config.getBoolean("shops.marker.enabled", true);
                String shopMarkerFormatLine1 = config.getString("shops.marker.formatLine1", "&e{title} &7({category})");
                String shopMarkerFormatLine2 = config.getString("shops.marker.formatLine2", "{status}");
                boolean shopMarkerUpdateOnChange = config.getBoolean("shops.marker.updateOnChange", true);
                boolean marketTeleportEnabled = config.getBoolean("market.teleport.enabled", true);
                String marketTeleportPermission = config.getString(
                                "market.teleport.permission",
                                "realite.city.market.tp");
                int marketTeleportCooldownSeconds = config.getInt("market.teleport.cooldownSeconds", 30);
                double marketTeleportCost = config.getDouble("market.teleport.cost", 0);
                int marketNearbyDefaultRadius = config.getInt("market.nearby.defaultRadius", 150);
                String marketHubWorld = config.getString("market.hub.world", "");
                double marketHubX = config.getDouble("market.hub.x", 0);
                double marketHubY = config.getDouble("market.hub.y", 0);
                double marketHubZ = config.getDouble("market.hub.z", 0);
                String language = config.getString("lang", config.getString("language", "ru"));

                return new CityConfig(
                                defaultPlotsPerPlayer,
                                homePlotsPerPlayer,
                                shopPlotsPerPlayer,
                                sqliteFile,
                                debug,
                                cityAreaDefaultDeny,
                                cityAreaBypassPermission,
                                homePlotsEnabled,
                                shopPlotsEnabled,
                                plotCleanupEnabled,
                                plotCleanupBlocksPerTick,
                                plotCleanupMode,
                                plotCleanupFillBlock,
                                plotCleanupClearAboveY,
                                plotNearbyDefaultRadius,
                                allowInteractOutsideMembers,
                                shopPointsPerPlot,
                                shopRentEnabled,
                                shopRentPeriodHours,
                                shopRentPricePerPeriod,
                                shopRentGraceHours,
                                shopRentCheckMinutes,
                                shopMarkerEnabled,
                                shopMarkerFormatLine1,
                                shopMarkerFormatLine2,
                                shopMarkerUpdateOnChange,
                                marketTeleportEnabled,
                                marketTeleportPermission,
                                marketTeleportCooldownSeconds,
                                marketTeleportCost,
                                marketNearbyDefaultRadius,
                                marketHubWorld,
                                marketHubX,
                                marketHubY,
                                marketHubZ,
                                language);
        }

        public int limitFor(ru.realite.city.model.PlotType type) {
                if (type == null) {
                        return defaultPlotsPerPlayer;
                }
                return switch (type) {
                        case HOME -> homePlotsPerPlayer;
                        case SHOP -> shopPlotsPerPlayer;
                };
        }
}
