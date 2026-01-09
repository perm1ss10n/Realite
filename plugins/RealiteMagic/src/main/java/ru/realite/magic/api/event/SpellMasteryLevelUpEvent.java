package ru.realite.magic.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class SpellMasteryLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String spellId;
    private final int newLevel;

    public SpellMasteryLevelUpEvent(UUID playerId, String spellId, int newLevel) {
        this.playerId = playerId;
        this.spellId = spellId;
        this.newLevel = newLevel;
    }

    public UUID playerId() {
        return playerId;
    }

    public String spellId() {
        return spellId;
    }

    public int newLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
