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
        return plotService.canInteract(player, location);
    }
}
