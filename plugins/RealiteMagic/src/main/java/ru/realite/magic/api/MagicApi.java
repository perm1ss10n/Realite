package ru.realite.magic.api;

import java.util.UUID;

/**
 * Public entry point for the magic system.
 * <p>
 * Implementations are provided by the plugin runtime; callers must treat returned
 * collections and views as immutable snapshots unless documented otherwise.
 */
public interface MagicApi {

    /**
     * Returns an immutable view of the loaded spell registry.
     */
    SpellRegistryView spellRegistry();

    /**
     * Access player spell management (learned spells, slots, selection).
     */
    PlayerSpellsApi playerSpells();

    /**
     * Access casting helpers for runtime casts.
     */
    MagicCastingApi casting();

    /**
     * Access public event contracts for magic events.
     */
    MagicEventsApi events();

    /**
     * Returns the current mastery level for the given player and spell.
     */
    int masteryLevel(UUID playerId, String spellId);
}
