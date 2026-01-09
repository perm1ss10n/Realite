package ru.realite.magic.target;

public record SpellTargetDefinition(
        SpellTargetType type,
        double maxDistance,
        boolean lineOfSight,
        boolean allowPlayers,
        boolean allowMobs
) {

    public static SpellTargetDefinition none() {
        return new SpellTargetDefinition(SpellTargetType.NONE, 0, true, true, true);
    }

    public boolean requiresTarget() {
        return type != null && type != SpellTargetType.NONE;
    }

    public double effectiveDistance(double fallback) {
        if (maxDistance > 0) {
            return maxDistance;
        }
        return fallback;
    }
}
