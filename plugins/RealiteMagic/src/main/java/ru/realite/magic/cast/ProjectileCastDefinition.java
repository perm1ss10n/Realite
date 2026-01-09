package ru.realite.magic.cast;

public record ProjectileCastDefinition(double speed,
                                       boolean gravity,
                                       double maxDistance,
                                       double hitRadius,
                                       ProjectileHitPolicy onHit) {
}
