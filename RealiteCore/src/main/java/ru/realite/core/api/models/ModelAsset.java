package ru.realite.core.api.models;

import java.util.Objects;

public record ModelAsset(String modelId,
                         ModelAssetKind kind,
                         ModelVisualProfile visualProfile,
                         ModelRendererHint rendererHint) {

    public ModelAsset {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(visualProfile, "visualProfile");
        Objects.requireNonNull(rendererHint, "rendererHint");
    }
}
