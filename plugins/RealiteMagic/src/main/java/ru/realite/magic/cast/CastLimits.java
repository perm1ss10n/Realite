package ru.realite.magic.cast;

import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;

public record CastLimits(int maxAoeTargets,
                         int maxChainTargets,
                         double maxBeamDistance,
                         double maxProjectileDistance) {

    public static CastLimits fromConfig(FileConfiguration config) {
        Objects.requireNonNull(config, "config");
        int maxAoeTargets = Math.max(1, config.getInt("limits.maxAoeTargets", 16));
        int maxChainTargets = Math.max(1, config.getInt("limits.maxChainTargets", 8));
        double maxBeamDistance = Math.max(1.0, config.getDouble("limits.maxBeamDistance", 30.0));
        double maxProjectileDistance = Math.max(1.0, config.getDouble("limits.maxProjectileDistance", 40.0));
        return new CastLimits(maxAoeTargets, maxChainTargets, maxBeamDistance, maxProjectileDistance);
    }
}
