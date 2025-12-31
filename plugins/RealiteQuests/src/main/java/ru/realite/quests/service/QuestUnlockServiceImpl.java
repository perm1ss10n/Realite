package ru.realite.quests.service;

import org.bukkit.entity.Player;
import ru.realite.core.api.quests.QuestUnlockService;

import java.util.Set;
import java.util.UUID;

public final class QuestUnlockServiceImpl implements QuestUnlockService {

    private final QuestUnlockRepository repository;

    public QuestUnlockServiceImpl(QuestUnlockRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasUnlock(Player player, String unlockId) {
        UUID uuid = resolve(player);
        if (uuid == null) {
            return false;
        }
        return repository.hasUnlock(uuid, unlockId);
    }

    @Override
    public void grantUnlock(Player player, String unlockId) {
        UUID uuid = resolve(player);
        if (uuid == null) {
            return;
        }
        repository.grantUnlock(uuid, unlockId);
    }

    @Override
    public Set<String> getUnlocks(Player player) {
        UUID uuid = resolve(player);
        if (uuid == null) {
            return Set.of();
        }
        return repository.getUnlocks(uuid);
    }

    private UUID resolve(Player player) {
        if (player == null) {
            return null;
        }
        return player.getUniqueId();
    }
}
