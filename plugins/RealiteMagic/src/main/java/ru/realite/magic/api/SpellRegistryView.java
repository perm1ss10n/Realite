package ru.realite.magic.api;

import java.util.Collection;
import java.util.Optional;

/**
 * Immutable registry view for loaded spells.
 */
public interface SpellRegistryView {

    /**
     * Finds a spell by id.
     *
     * @param spellId spell identifier (case-insensitive)
     * @return immutable spell view if present
     */
    Optional<SpellView> find(String spellId);

    /**
     * Returns an immutable snapshot of all loaded spells.
     */
    Collection<SpellView> all();
}
