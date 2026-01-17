package ru.realite.core.api.models;

import java.util.Objects;

public record ModelVisualProfile(double scale, ModelOffset offset, String anchor) {

    public ModelVisualProfile {
        Objects.requireNonNull(offset, "offset");
        Objects.requireNonNull(anchor, "anchor");
    }
}
