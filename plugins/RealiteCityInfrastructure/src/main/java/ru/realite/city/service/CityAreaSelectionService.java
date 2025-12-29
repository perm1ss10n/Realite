package ru.realite.city.service;

import org.bukkit.Location;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CityAreaSelectionService {

    public record Selection(Location pos1, Location pos2) {
        public Selection withPos1(Location location) {
            return new Selection(location, pos2);
        }

        public Selection withPos2(Location location) {
            return new Selection(pos1, location);
        }
    }

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();
    private final Set<UUID> wandEnabled = ConcurrentHashMap.newKeySet();

    public void enableWand(UUID playerId) {
        wandEnabled.add(playerId);
    }

    public boolean isWandEnabled(UUID playerId) {
        return wandEnabled.contains(playerId);
    }

    public void disableWand(UUID playerId) {
        wandEnabled.remove(playerId);
    }

    public void setPos1(UUID playerId, Location location) {
        selections.merge(playerId, new Selection(location, null), (prev, next) -> prev.withPos1(location));
    }

    public void setPos2(UUID playerId, Location location) {
        selections.merge(playerId, new Selection(null, location), (prev, next) -> prev.withPos2(location));
    }

    public Optional<Selection> getSelection(UUID playerId) {
        return Optional.ofNullable(selections.get(playerId));
    }
}
