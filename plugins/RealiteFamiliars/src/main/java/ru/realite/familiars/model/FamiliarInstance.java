package ru.realite.familiars.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record FamiliarInstance(
        UUID owner,
        String typeId,
        int level,
        int xp,
        FamiliarState state,
        Optional<UUID> summonedEntityId
) {
    public FamiliarInstance {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(state, "state");
        summonedEntityId = summonedEntityId == null ? Optional.empty() : summonedEntityId;
    }
}
