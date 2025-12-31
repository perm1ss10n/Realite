package ru.realite.city.service;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.model.Plot;
import ru.realite.core.api.integrations.AccessResult;
import ru.realite.core.api.integrations.CityAccessHook;
import ru.realite.core.api.integrations.city.Action;
import ru.realite.core.api.integrations.city.PlotRef;

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
            return plotService.checkAccess(null, location, ru.realite.city.service.Action.INTERACT).isAllowed();
        }
        return plotService.checkAccess(player.getUniqueId(), location, ru.realite.city.service.Action.INTERACT).isAllowed();
    }

    @Override
    public Optional<String> getPlotIdAt(Location location) {
        if (plotService == null) {
            return Optional.empty();
        }
        return plotService.findContaining(location).map(Plot::id);
    }

    @Override
    public Optional<PlotRef> getPlotAt(Location location) {
        if (plotService == null) {
            return Optional.empty();
        }
        return plotService.findContaining(location).map(plot -> new PlotRef(plot.id()));
    }

    @Override
    public boolean isInCityPlot(Location location) {
        if (plotService == null) {
            return false;
        }
        return plotService.findContaining(location).isPresent();
    }

    @Override
    public AccessResult checkAccess(UUID playerId, Location location, Action action) {
        if (plotService == null) {
            return AccessResult.allow();
        }
        return mapResult(plotService.checkAccess(playerId, location, mapAction(action)));
    }

    private ru.realite.city.service.Action mapAction(Action action) {
        if (action == null) {
            return ru.realite.city.service.Action.INTERACT;
        }
        return switch (action) {
            case MODIFY -> ru.realite.city.service.Action.MODIFY;
            case INTERACT -> ru.realite.city.service.Action.INTERACT;
            case EXPLOSION -> ru.realite.city.service.Action.EXPLOSION;
        };
    }

    private AccessResult mapResult(ru.realite.city.service.AccessResult result) {
        if (result == null) {
            return AccessResult.allow();
        }
        return result.isAllowed() ? AccessResult.allow() : AccessResult.deny(result.reasonKey());
    }
}
