package ru.realite.magic.api;

import java.util.Optional;
import java.util.UUID;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;

/**
 * Player spell management API.
 */
public interface PlayerSpellsApi {

    /**
     * Returns whether the player has learned the given spell.
     */
    boolean hasSpell(UUID playerId, String spellId);

    /**
     * Unlocks a spell for the player and fires public events.
     */
    UnlockResult unlock(UUID playerId, String spellId, SpellUnlockSource source);

    /**
     * Selects a spell for the player.
     */
    SelectResult select(UUID playerId, String spellId);

    /**
     * Returns the currently selected spell id, if any.
     */
    Optional<String> selected(UUID playerId);
}
