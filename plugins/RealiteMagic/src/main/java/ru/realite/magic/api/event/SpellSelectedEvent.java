package ru.realite.magic.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player changes the selected spell.
 */
public final class SpellSelectedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    @Nullable
    private final String previousSpellId;
    @Nullable
    private final String newSpellId;

    public SpellSelectedEvent(UUID playerId, @Nullable String previousSpellId, @Nullable String newSpellId) {
        this.playerId = playerId;
        this.previousSpellId = previousSpellId;
        this.newSpellId = newSpellId;
    }

    public UUID playerId() {
        return playerId;
    }

    public @Nullable String previousSpellId() {
        return previousSpellId;
    }

    public @Nullable String newSpellId() {
        return newSpellId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
