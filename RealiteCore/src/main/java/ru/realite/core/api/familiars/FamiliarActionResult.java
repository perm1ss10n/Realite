package ru.realite.core.api.familiars;

import java.util.List;
import java.util.Objects;

public record FamiliarActionResult(boolean allowed, List<String> reasons) {

    public FamiliarActionResult {
        Objects.requireNonNull(reasons, "reasons");
    }

    /* ---------- Factory methods ---------- */

    public static FamiliarActionResult success() {
        return new FamiliarActionResult(true, List.of());
    }

    public static FamiliarActionResult denied(List<String> reasons) {
        return new FamiliarActionResult(false, reasons == null ? List.of() : List.copyOf(reasons));
    }

    /* ---------- Convenience ---------- */

    public boolean denied() {
        return !allowed;
    }
}