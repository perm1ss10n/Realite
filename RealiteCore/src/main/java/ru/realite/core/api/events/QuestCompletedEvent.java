package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class QuestCompletedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String questId;

    public QuestCompletedEvent(UUID playerUuid, String questId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.questId = Objects.requireNonNull(questId, "questId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String questId() {
        return questId;
    }
}
