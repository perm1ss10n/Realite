package ru.realite.core.api.integrations;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface CityAccessHook {
    boolean canInteract(Player player, Location location);
}
