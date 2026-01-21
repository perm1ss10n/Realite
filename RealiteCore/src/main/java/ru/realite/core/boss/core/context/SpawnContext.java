package ru.realite.core.boss.core.context;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public record SpawnContext(Location location, Player initiator) {
    public SpawnContext {
        if (location == null) {
            throw new IllegalArgumentException("location is null");
        }
    }
}
