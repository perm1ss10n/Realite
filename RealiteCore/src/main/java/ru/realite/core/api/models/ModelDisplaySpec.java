package ru.realite.core.api.models;

import java.util.Objects;
import org.bukkit.Material;

public record ModelDisplaySpec(Material material, Integer customModelData) {

    public ModelDisplaySpec {
        Objects.requireNonNull(material, "material");
    }
}
