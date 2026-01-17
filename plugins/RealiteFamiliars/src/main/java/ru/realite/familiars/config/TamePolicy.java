package ru.realite.familiars.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TamePolicy(Map<String, List<String>> allowedMobsByClass) {

    public TamePolicy {
        Objects.requireNonNull(allowedMobsByClass, "allowedMobsByClass");
    }

    public Optional<List<String>> allowedMobs(String classId) {
        if (classId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(allowedMobsByClass.get(classId.toLowerCase(Locale.ROOT)));
    }
}
