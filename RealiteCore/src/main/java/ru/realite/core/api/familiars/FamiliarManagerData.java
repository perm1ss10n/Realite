package ru.realite.core.api.familiars;

import java.util.List;
import java.util.Optional;

public record FamiliarManagerData(List<FamiliarSummary> familiars, Optional<String> activeTypeId) {
    public FamiliarManagerData {
        familiars = familiars == null ? List.of() : List.copyOf(familiars);
        activeTypeId = activeTypeId == null ? Optional.empty() : activeTypeId;
    }
}
