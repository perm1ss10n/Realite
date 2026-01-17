package ru.realite.core.api.familiars;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Objects;

public record FamiliarHudActive(
        String typeId,
        String name,
        int level,
        String role,
        Optional<String> modelId,
        OptionalInt hpCurrent,
        OptionalInt hpMax,
        OptionalDouble distanceMeters
) {
    public FamiliarHudActive {
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
        modelId = modelId == null ? Optional.empty() : modelId;
        hpCurrent = hpCurrent == null ? OptionalInt.empty() : hpCurrent;
        hpMax = hpMax == null ? OptionalInt.empty() : hpMax;
        distanceMeters = distanceMeters == null ? OptionalDouble.empty() : distanceMeters;
    }
}
