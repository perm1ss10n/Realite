package ru.realite.city.gui;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface CityMenuAction {
    void execute(Player player);
}
