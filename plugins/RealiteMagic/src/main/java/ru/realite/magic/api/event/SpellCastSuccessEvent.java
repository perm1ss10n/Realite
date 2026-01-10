package ru.realite.magic.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Fired after a spell successfully completes casting.
 * <p>
 * The target id is optional and may be {@code null} for non-entity targets.
 */
public final class SpellCastSuccessEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String spellId;
    @Nullable
    private final UUID targetId;

    public SpellCastSuccessEvent(UUID playerId, String spellId) {
        this(playerId, spellId, null);
    }

    public SpellCastSuccessEvent(UUID playerId, String spellId, @Nullable UUID targetId) {
        this.playerId = playerId;
        this.spellId = spellId;
        this.targetId = targetId;
    }

    public UUID playerId() {
        return playerId;
    }

    public String spellId() {
        return spellId;
    }

    public @Nullable UUID targetId() {
        return targetId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
