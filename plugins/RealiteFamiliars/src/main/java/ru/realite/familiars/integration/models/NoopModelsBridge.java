package ru.realite.familiars.integration.models;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.entity.Entity;
import ru.realite.core.api.models.ApplyResult;
import ru.realite.core.api.models.ModelContext;
import ru.realite.core.api.models.ModelInfo;
import ru.realite.core.api.models.ModelsBridge;
import ru.realite.familiars.integration.BridgeWarning;

public final class NoopModelsBridge implements ModelsBridge {

    private final BridgeWarning warning;

    public NoopModelsBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] ModelsBridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ApplyResult apply(Entity target, String modelId, ModelContext ctx) {
        warning.warnOnce();
        return ApplyResult.fail("Models bridge not available.");
    }

    @Override
    public void clear(Entity target) {
        warning.warnOnce();
    }

    @Override
    public Optional<ModelInfo> getApplied(Entity target) {
        return Optional.empty();
    }
}
