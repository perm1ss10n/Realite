package ru.realite.core.api;

import java.util.Objects;
import java.util.Set;

/**
 * Метаданные модуля.
 */
public record ModuleMetadata(
        ModuleId id,
        String name,
        String version,
        Set<ModuleId> dependencies
) {
    public ModuleMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(dependencies, "dependencies");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Module name is blank");
        }
        if (version.isBlank()) {
            throw new IllegalArgumentException("Module version is blank");
        }
        dependencies = Set.copyOf(dependencies);
    }
}
