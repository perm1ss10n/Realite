package ru.realite.familiars.config;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public record FamiliarLimits(int defaultLimit, Map<String, Map<Integer, Integer>> classTierLimits) {

    public FamiliarLimits {
        Objects.requireNonNull(classTierLimits, "classTierLimits");
    }

    public OptionalInt getLimit(String classId, int tier) {
        if (classId == null || classId.isBlank()) {
            return OptionalInt.empty();
        }
        Map<Integer, Integer> tiers = classTierLimits.get(classId.toLowerCase(Locale.ROOT));
        if (tiers == null) {
            return OptionalInt.empty();
        }
        Integer limit = tiers.get(tier);
        if (limit == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(limit);
    }
}
