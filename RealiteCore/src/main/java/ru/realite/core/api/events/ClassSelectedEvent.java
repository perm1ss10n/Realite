package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class ClassSelectedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String classId;

    public ClassSelectedEvent(UUID playerUuid, String classId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String classId() {
        return classId;
    }
}
