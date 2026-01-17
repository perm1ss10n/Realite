package ru.realite.core.api.models;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record ModelContext(String source, Map<String, String> attributes) {

    public ModelContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(attributes, "attributes");
    }

    public static ModelContext empty() {
        return new ModelContext("unknown", Collections.emptyMap());
    }
}
