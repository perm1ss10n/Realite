package ru.realite.city.service;

import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.model.PlotType;

public final class DefaultCityHooks implements CityHooks {

    private final CityConfig config;

    public DefaultCityHooks(CityConfig config) {
        this.config = config;
    }

    @Override
    public int maxPlots(Player player, PlotType type) {
        return config.limitFor(type);
    }

    @Override
    public double marketTeleportCostMultiplier(Player player) {
        return 1.0;
    }

    @Override
    public boolean canUseMarketTeleport(Player player) {
        String permission = config.marketTeleportPermission();
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }
}
