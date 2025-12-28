package ru.realite.city;

import ru.realite.core.api.Config;

public record CityConfig(
        int defaultPlotsPerPlayer,
        String sqliteFile,
        boolean debug,
        boolean cityAreaDefaultDeny,
        String cityAreaBypassPermission,
        boolean homePlotsEnabled,
        boolean shopPlotsEnabled
) {
    public static CityConfig from(Config config) {
        int defaultPlotsPerPlayer = config.getInt("limits.defaultPlotsPerPlayer", 3);
        String sqliteFile = config.getString("storage.sqliteFile", "city.sqlite");
        boolean debug = config.getBoolean("logging.debug", false);
        boolean cityAreaDefaultDeny = config.getBoolean("cityArea.defaultDeny", true);
        String cityAreaBypassPermission = config.getString(
                "cityArea.bypassPermission",
                "realite.city.bypass"
        );
        boolean homePlotsEnabled = config.getBoolean("plots.types.home.enabled", true);
        boolean shopPlotsEnabled = config.getBoolean("plots.types.shop.enabled", true);
        return new CityConfig(
                defaultPlotsPerPlayer,
                sqliteFile,
                debug,
                cityAreaDefaultDeny,
                cityAreaBypassPermission,
                homePlotsEnabled,
                shopPlotsEnabled
        );
    }
}
