package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.core.api.integrations.CityAccessHook;

public final class CityInfrastructureAccessHook implements CityAccessHook {

    private final PlotService plotService;

    public CityInfrastructureAccessHook(PlotService plotService) {
        this.plotService = plotService;
    }

    @Override
    public boolean canInteract(Player player, Location location) {
        if (plotService == null) {
            return true;
        }
        if (player == null) {
            return plotService.checkAccess(null, location, Action.INTERACT).isAllowed();
        }
        return plotService.checkAccess(player.getUniqueId(), location, Action.INTERACT).isAllowed();
    }
}
