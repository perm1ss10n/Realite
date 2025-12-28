package ru.realite.classes.event;

import ru.realite.classes.model.ClassId;
import ru.realite.core.api.events.CoreEvent;

import java.util.Objects;
import java.util.UUID;

public final class EvolutionCompletedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final ClassId classId;
    private final String evolutionId;

    public EvolutionCompletedEvent(UUID playerUuid, ClassId classId, String evolutionId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
        this.evolutionId = Objects.requireNonNull(evolutionId, "evolutionId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public ClassId classId() {
        return classId;
    }

    public String evolutionId() {
        return evolutionId;
    }
}
