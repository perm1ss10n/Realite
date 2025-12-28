package ru.realite.classes.event;

import ru.realite.classes.model.ClassId;
import ru.realite.core.api.events.CoreEvent;

import java.util.Objects;
import java.util.UUID;

public final class ClassSelectedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final ClassId classId;

    public ClassSelectedEvent(UUID playerUuid, ClassId classId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public ClassId classId() {
        return classId;
    }
}
