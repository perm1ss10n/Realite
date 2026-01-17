package ru.realite.core.api.familiars;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record FamiliarDetailsData(
        String typeId,
        String name,
        String mobType,
        int level,
        int xp,
        int xpMax,
        String role,
        Optional<String> modelId,
        FamiliarUiState state,
        Map<String, Integer> stats,
        List<String> talents,
        boolean inventoryEnabled,
        List<String> inventory
) {
    public FamiliarDetailsData {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mobType, "mobType");
        Objects.requireNonNull(role, "role");
        modelId = modelId == null ? Optional.empty() : modelId;
        Objects.requireNonNull(state, "state");
        stats = stats == null ? Map.of() : Map.copyOf(stats);
        talents = talents == null ? List.of() : List.copyOf(talents);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
    }
}
