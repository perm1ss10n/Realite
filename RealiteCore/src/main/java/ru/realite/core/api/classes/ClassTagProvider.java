package ru.realite.core.api.classes;

import org.bukkit.entity.Player;

/**
 * Провайдер тега класса для игрока.
 */
public interface ClassTagProvider {

    ClassTag getTag(Player player);
}
