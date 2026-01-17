package ru.realite.models.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import ru.realite.core.api.models.ApplyResult;
import ru.realite.core.api.models.ModelContext;
import ru.realite.core.api.models.ModelInfo;
import ru.realite.core.api.models.ModelsBridge;

public final class ModelsBridgeImpl implements ModelsBridge {

    private final Map<UUID, ModelInfo> applied = new ConcurrentHashMap<>();

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ApplyResult apply(Entity target, String modelId, ModelContext ctx) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(ctx, "ctx");

        applied.put(target.getUniqueId(), new ModelInfo(modelId, ctx));
        return ApplyResult.ok();
    }

    @Override
    public void clear(Entity target) {
        Objects.requireNonNull(target, "target");
        applied.remove(target.getUniqueId());
    }

    @Override
    public Optional<ModelInfo> getApplied(Entity target) {
        Objects.requireNonNull(target, "target");
        return Optional.ofNullable(applied.get(target.getUniqueId()));
    }
}
