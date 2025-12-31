package ru.realite.core.api.quests;

import org.bukkit.entity.Player;

/**
 * Сервис запуска квестов.
 */
public interface QuestService {

    void start(Player player, String questId);
}
