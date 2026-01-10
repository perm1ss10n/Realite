package ru.realite.magic.integration.city;

import java.util.Optional;
import org.bukkit.Location;

public interface CityBridge {
    boolean isAvailable();

    Optional<RegionInfo> regionAt(Location location);
}
