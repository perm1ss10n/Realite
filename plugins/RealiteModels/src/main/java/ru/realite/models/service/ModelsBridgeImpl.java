package ru.realite.models.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.bukkit.entity.Entity;
import ru.realite.core.api.models.ApplyResult;
import ru.realite.core.api.models.ModelContext;
import ru.realite.core.api.models.ModelAssetInfo;
import ru.realite.core.api.models.ModelAssetKind;
import ru.realite.core.api.models.ModelAssetRegistry;
import ru.realite.core.api.models.ModelInfo;
import ru.realite.core.api.models.ModelRendererHint;
import ru.realite.core.api.models.ModelsBridge;

public final class ModelsBridgeImpl implements ModelsBridge {

    private final Map<UUID, ModelInfo> applied = new ConcurrentHashMap<>();
    private final Supplier<ModelAssetRegistry> registrySupplier;
    private final ModelWrapperService wrapperService;

    public ModelsBridgeImpl(Supplier<ModelAssetRegistry> registrySupplier, ModelWrapperService wrapperService) {
        this.registrySupplier = Objects.requireNonNull(registrySupplier, "registrySupplier");
        this.wrapperService = Objects.requireNonNull(wrapperService, "wrapperService");
        this.wrapperService.setAppliedRemover(this::forget);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public ApplyResult apply(Entity target, String modelId, ModelContext ctx) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(ctx, "ctx");

        ModelAssetRegistry registry = registrySupplier.get();
        if (registry == null) {
            return ApplyResult.fail("Model assets registry is not available.");
        }
        ModelAssetInfo assetInfo = registry.find(modelId).orElse(null);
        if (assetInfo == null) {
            return ApplyResult.fail("Model asset not found: " + modelId);
        }
        if (assetInfo.asset().kind() != ModelAssetKind.ENTITY) {
            return ApplyResult.fail("Model asset kind must be ENTITY for model: " + modelId);
        }
        if (assetInfo.asset().rendererHint() == ModelRendererHint.DISPLAY) {
            var result = wrapperService.apply(target, assetInfo);
            if (!result.success()) {
                return result;
            }
        } else if (assetInfo.asset().rendererHint() != ModelRendererHint.NONE) {
            return ApplyResult.fail("Model renderer hint is not supported for model: " + modelId);
        }

        applied.put(target.getUniqueId(), new ModelInfo(modelId, ctx));
        return ApplyResult.ok();
    }

    @Override
    public void clear(Entity target) {
        Objects.requireNonNull(target, "target");
        wrapperService.clear(target);
        applied.remove(target.getUniqueId());
    }

    @Override
    public Optional<ModelInfo> getApplied(Entity target) {
        Objects.requireNonNull(target, "target");
        return Optional.ofNullable(applied.get(target.getUniqueId()));
    }

    private void forget(UUID targetId) {
        applied.remove(targetId);
    }
}
