package ru.realite.core.boss.core.context;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public record SpawnContext(Location location, Optional<Player> initiator) {
    public SpawnContext {
        if (location == null) {
            throw new IllegalArgumentException("location is null");
        }
        initiator = initiator == null ? Optional.empty() : initiator;
    }

    public static SpawnContext of(Location location, Player initiator) {
        return new SpawnContext(location, Optional.ofNullable(initiator));
    }
}
