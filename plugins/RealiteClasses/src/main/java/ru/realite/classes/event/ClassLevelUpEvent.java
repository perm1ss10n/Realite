package ru.realite.classes.event;

import ru.realite.classes.model.ClassId;
import ru.realite.core.api.events.CoreEvent;

import java.util.Objects;
import java.util.UUID;

public final class ClassLevelUpEvent implements CoreEvent {
    private final UUID playerUuid;
    private final ClassId classId;
    private final int newLevel;

    public ClassLevelUpEvent(UUID playerUuid, ClassId classId, int newLevel) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.classId = Objects.requireNonNull(classId, "classId");
        this.newLevel = newLevel;
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public ClassId classId() {
        return classId;
    }

    public int newLevel() {
        return newLevel;
    }
}
