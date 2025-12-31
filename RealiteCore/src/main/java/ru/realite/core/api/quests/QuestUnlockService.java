package ru.realite.core.api.quests;

import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Хранит флаги разблокировок, полученные из квестов.
 */
public interface QuestUnlockService {

    boolean hasUnlock(Player player, String unlockId);

    void grantUnlock(Player player, String unlockId);

    Set<String> getUnlocks(Player player);
}
