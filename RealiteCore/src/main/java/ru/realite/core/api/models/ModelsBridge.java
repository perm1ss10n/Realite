package ru.realite.core.api.models;

import java.util.Optional;
import org.bukkit.entity.Entity;

public interface ModelsBridge {

    boolean isAvailable();

    ApplyResult apply(Entity target, String modelId, ModelContext ctx);

    void clear(Entity target);

    Optional<ModelInfo> getApplied(Entity target);
}
