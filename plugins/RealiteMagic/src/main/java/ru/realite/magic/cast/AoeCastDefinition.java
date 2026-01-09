package ru.realite.magic.cast;

public record AoeCastDefinition(double radius,
                                int maxTargets,
                                boolean includePlayers,
                                boolean includeMobs) {
}
