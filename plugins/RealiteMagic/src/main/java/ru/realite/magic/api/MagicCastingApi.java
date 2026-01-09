package ru.realite.magic.api;

import org.bukkit.entity.Player;
import ru.realite.magic.cast.CastAttemptResult;

/**
 * Public casting helpers for runtime spell execution.
 */
public interface MagicCastingApi {

    /**
     * Attempts to cast the selected spell for the given player.
     */
    CastAttemptResult tryCastSelected(Player player);

    /**
     * Attempts to cast a spell by id for the given player.
     */
    CastAttemptResult tryCast(Player player, String spellId);
}
