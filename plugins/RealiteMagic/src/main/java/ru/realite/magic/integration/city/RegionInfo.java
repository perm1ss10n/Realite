package ru.realite.magic.integration.city;

import org.jetbrains.annotations.Nullable;

public record RegionInfo(
        String regionId,
        RegionType type,
        @Nullable String cityId,
        @Nullable String plotId
) {
}
