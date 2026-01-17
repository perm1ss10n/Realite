package ru.realite.familiars.integration.quests;

import java.util.Objects;
import java.util.UUID;

public record FamiliarQuestXpEvent(
        UUID ownerId,
        String familiarTypeId,
        int amount
) {
    public FamiliarQuestXpEvent {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(familiarTypeId, "familiarTypeId");
    }
}
