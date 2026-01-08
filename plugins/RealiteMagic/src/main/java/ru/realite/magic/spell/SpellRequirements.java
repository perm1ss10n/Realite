package ru.realite.magic.spell;

public record SpellRequirements(String classId,
                                String evolutionId,
                                String requiredItemId,
                                boolean consumeOnCast) {

    public boolean isEmpty() {
        return (classId == null || classId.isBlank())
                && (evolutionId == null || evolutionId.isBlank())
                && (requiredItemId == null || requiredItemId.isBlank());
    }
}
