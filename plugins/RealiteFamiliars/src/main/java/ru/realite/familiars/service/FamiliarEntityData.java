package ru.realite.familiars.service;

import java.util.Objects;
import java.util.UUID;

public record FamiliarEntityData(UUID ownerId, String typeId) {
    public FamiliarEntityData {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(typeId, "typeId");
    }
}
