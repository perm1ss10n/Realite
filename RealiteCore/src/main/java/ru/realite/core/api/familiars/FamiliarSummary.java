package ru.realite.core.api.familiars;

import java.util.Objects;

public record FamiliarSummary(
        String typeId,
        String name,
        String mobType,
        int level,
        String role,
        FamiliarUiState state
) {
    public FamiliarSummary {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mobType, "mobType");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(state, "state");
    }
}
