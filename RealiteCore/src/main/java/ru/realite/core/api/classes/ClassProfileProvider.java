package ru.realite.core.api.classes;

import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * Провайдер профиля класса игрока.
 */
public interface ClassProfileProvider {

    default Optional<ClassProfile> getProfile(Player player) {
        return Optional.empty();
    }
}
