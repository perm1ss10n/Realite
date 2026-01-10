package ru.realite.magic.spell;

import java.util.List;

public final class SpellLoadReport {

    private final int loadedCount;
    private final List<SpellLoadError> errors;

    public SpellLoadReport(int loadedCount, List<SpellLoadError> errors) {
        this.loadedCount = loadedCount;
        this.errors = List.copyOf(errors);
    }

    public int loadedCount() {
        return loadedCount;
    }

    public List<SpellLoadError> errors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
