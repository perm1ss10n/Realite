package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.model.CityArea;
import ru.realite.city.storage.CityAreaRepository;

import java.util.Optional;

public final class CityProtectionService {

    private static final String ADMIN_PERMISSION = "realite.city.admin";

    private final CityConfig config;
    private final CityAreaRepository repository;

    public CityProtectionService(CityConfig config, CityAreaRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    public boolean isInCityArea(Location location) {
        return repository.findContaining(location).isPresent();
    }

    public Optional<CityArea> findContaining(Location location) {
        return repository.findContaining(location);
    }

    public boolean canModify(Player player, Location location) {
        if (player == null || location == null) {
            return true;
        }
        if (!config.cityAreaDefaultDeny()) {
            return true;
        }
        if (!isInCityArea(location)) {
            return true;
        }
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        String bypassPermission = config.cityAreaBypassPermission();
        return bypassPermission != null && !bypassPermission.isBlank() && player.hasPermission(bypassPermission);
    }
}
