package ru.realite.magic.spell;

public record SpellRequirements(String classId, String evolutionId) {

    public boolean isEmpty() {
        return (classId == null || classId.isBlank())
                && (evolutionId == null || evolutionId.isBlank());
    }
}
