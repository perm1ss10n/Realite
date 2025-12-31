package ru.realite.core.api.quests;

import org.bukkit.entity.Player;

/**
 * Сервис запуска квестов.
 */
public interface QuestService {

    default void start(Player player, String questId) {
        start(player, questId, QuestStartTrigger.COMMAND, false);
    }

    void start(Player player, String questId, QuestStartTrigger trigger, boolean force);

    boolean isActive(Player player, String questId);

    QuestProgress getProgress(Player player, String questId);
}
