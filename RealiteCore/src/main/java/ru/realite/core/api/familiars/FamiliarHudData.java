package ru.realite.core.api.familiars;

import java.util.Optional;

public record FamiliarHudData(int count, int max, Optional<FamiliarHudActive> active) {
    public FamiliarHudData {
        active = active == null ? Optional.empty() : active;
    }
}
