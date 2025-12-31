package ru.realite.core.api.quests;

import java.util.Optional;
import org.bukkit.entity.Player;

/**
 * Адаптер для проверки условий, связанных с гильдиями.
 */
public interface GuildAdapter {

    /**
     * Проверяет, состоит ли игрок в гильдии.
     */
    default boolean isInGuild(Player player) {
        return false;
    }

    /**
     * Возвращает тег гильдии игрока.
     */
    default Optional<String> getGuildTag(Player player) {
        return Optional.empty();
    }

    /**
     * Возвращает идентификатор ранга игрока в гильдии.
     */
    default Optional<String> getGuildRankId(Player player) {
        return Optional.empty();
    }
}
