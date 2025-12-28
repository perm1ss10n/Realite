package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class EvolutionCompletedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String classId;
    private final String evolutionId;

    public EvolutionCompletedEvent(UUID playerUuid, String classId, String evolutionId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
        this.evolutionId = Objects.requireNonNull(evolutionId, "evolutionId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String classId() {
        return classId;
    }

    public String evolutionId() {
        return evolutionId;
    }
}
