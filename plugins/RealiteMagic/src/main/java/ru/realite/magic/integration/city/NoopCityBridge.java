package ru.realite.magic.integration.city;

import java.util.Optional;
import org.bukkit.Location;

public final class NoopCityBridge implements CityBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<RegionInfo> regionAt(Location location) {
        return Optional.empty();
    }
}
