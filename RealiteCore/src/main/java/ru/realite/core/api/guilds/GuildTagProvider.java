package ru.realite.core.api.guilds;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Провайдер тега гильдии для игрока.
 */
public interface GuildTagProvider {

    default Optional<Component> getTag(Player player) {
        return Optional.empty();
    }

    default Optional<Component> getHover(Player player) {
        return Optional.empty();
    }
}
