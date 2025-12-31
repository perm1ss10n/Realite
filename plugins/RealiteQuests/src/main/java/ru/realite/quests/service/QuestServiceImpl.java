package ru.realite.quests.service;

import org.bukkit.entity.Player;
import ru.realite.core.api.Platform;
import ru.realite.core.api.quests.QuestService;

import java.util.Objects;

public final class QuestServiceImpl implements QuestService {

    private final Platform logger;

    public QuestServiceImpl(Platform logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void start(Player player, String questId) {
        if (player == null || questId == null || questId.isBlank()) {
            return;
        }
        logger.info("[Quests] TODO start quest " + questId + " for " + player.getName());
    }
}
