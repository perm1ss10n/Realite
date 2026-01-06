package ru.realite.core.api;

import java.util.Objects;
import java.util.Set;

/**
 * Метаданные модуля.
 */
public record ModuleMetadata(
        ModuleId id,
        Set<ModuleId> dependencies
) {
    public ModuleMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dependencies, "dependencies");
        dependencies = Set.copyOf(dependencies);
    }
}
