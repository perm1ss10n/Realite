package ru.realite.magic.cast;

public record BeamCastDefinition(double maxDistance,
                                 double step,
                                 double hitRadius,
                                 BeamParticlesDefinition particles) {
}
