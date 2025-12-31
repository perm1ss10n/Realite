package ru.realite.core.api.integrations;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.core.api.integrations.city.Action;
import ru.realite.core.api.integrations.city.PlotRef;

public interface CityAccessHook {
    boolean canInteract(Player player, Location location);

    default Optional<String> getPlotIdAt(Location location) {
        return Optional.empty();
    }

    default Optional<PlotRef> getPlotAt(Location location) {
        return Optional.empty();
    }

    default boolean isInCityPlot(Location location) {
        return false;
    }

    default AccessResult checkAccess(UUID playerId, Location location, Action action) {
        return AccessResult.allow();
    }
}
