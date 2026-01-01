package ru.realite.core.api.classes;

import org.bukkit.entity.Player;

/**
 * Сервис начисления опыта класса.
 */
public interface ClassXpService {

    void addXp(Player player, long amount);
}
