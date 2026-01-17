package ru.realite.core.api.models;

import java.util.Map;
import java.util.Optional;

public interface ModelAssetRegistry {

    Optional<ModelAssetInfo> find(String modelId);

    Map<String, ModelAssetInfo> all();
}
