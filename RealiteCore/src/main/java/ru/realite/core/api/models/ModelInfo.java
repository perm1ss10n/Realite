package ru.realite.core.api.models;

import java.util.Objects;

public record ModelInfo(String modelId, ModelContext context) {

    public ModelInfo {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(context, "context");
    }
}
