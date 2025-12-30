package ru.realite.guilds.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.core.api.integrations.CityAccessHook;

public final class NoopCityAccessHook implements CityAccessHook {
    @Override
    public boolean canInteract(Player player, Location location) {
        return true;
    }
}
