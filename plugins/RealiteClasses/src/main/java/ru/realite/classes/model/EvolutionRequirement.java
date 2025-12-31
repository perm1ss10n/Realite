package ru.realite.classes.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public record EvolutionRequirement(Set<ClassId> masteredClasses) {

    public EvolutionRequirement {
        masteredClasses = masteredClasses == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(masteredClasses));
    }

    public boolean isEmpty() {
        return masteredClasses == null || masteredClasses.isEmpty();
    }

    public static EvolutionRequirement fromMastered(Set<ClassId> masteredClasses) {
        if (masteredClasses == null || masteredClasses.isEmpty()) {
            return null;
        }
        return new EvolutionRequirement(masteredClasses);
    }
}
