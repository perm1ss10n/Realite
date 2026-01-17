package ru.realite.familiars.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record FamiliarType(
        String id,
        String role,
        String displayKey,
        Optional<String> modelId,
        List<String> allowedClasses,
        Map<String, Integer> baseStats
) {
    public FamiliarType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(displayKey, "displayKey");
        modelId = modelId == null ? Optional.empty() : modelId;
        Objects.requireNonNull(allowedClasses, "allowedClasses");
        Objects.requireNonNull(baseStats, "baseStats");
    }
}
