package ru.realite.core.api.models;

import java.util.Objects;

public record ModelAssetInfo(ModelAsset asset, String source) {

    public ModelAssetInfo {
        Objects.requireNonNull(asset, "asset");
        Objects.requireNonNull(source, "source");
    }
}
