package ru.realite.magic.cast;

public record ChainCastDefinition(int jumps,
                                  double jumpRange,
                                  boolean includePlayers,
                                  boolean includeMobs) {
}
