package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class ClassLevelUpEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String classId;
    private final int newLevel;

    public ClassLevelUpEvent(UUID playerUuid, String classId, int newLevel) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
        this.newLevel = newLevel;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String classId() {
        return classId;
    }

    public int newLevel() {
        return newLevel;
    }
}
