package ru.realite.familiars.integration.quests;

import java.util.Objects;
import java.util.UUID;

public record FamiliarQuestEvent(
        FamiliarQuestEventType type,
        UUID ownerId,
        String familiarTypeId,
        int level
) {
    public FamiliarQuestEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(familiarTypeId, "familiarTypeId");
    }
}
