package ru.realite.city.service;

import org.bukkit.entity.Player;
import ru.realite.city.model.PlotType;

public interface CityHooks {
    int maxPlots(Player player, PlotType type);

    double marketTeleportCostMultiplier(Player player);

    boolean canUseMarketTeleport(Player player);
}
