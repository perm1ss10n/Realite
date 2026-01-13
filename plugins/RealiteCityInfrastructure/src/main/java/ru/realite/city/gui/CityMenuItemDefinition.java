package ru.realite.city.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.city.i18n.CityMessages;

import java.util.function.Function;
import java.util.function.Predicate;

public record CityMenuItemDefinition(
        String id,
        int slot,
        Material material,
        String permission,
        Predicate<CityMainMenu.HubContext> condition,
        Function<CityMessages, String> unavailableReasonKey,
        CityMenuAction action
) {
    public boolean isAvailable(Player player, CityMainMenu.HubContext context) {
        boolean hasPermission = permission == null || permission.isBlank() || player.hasPermission(permission);
        boolean conditionOk = condition == null || condition.test(context);
        return hasPermission && conditionOk;
    }
}
