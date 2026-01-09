package ru.realite.magic.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.realite.magic.service.SpellUnlockSource;

public final class SpellUnlockedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String spellId;
    private final SpellUnlockSource source;

    public SpellUnlockedEvent(UUID playerId, String spellId, SpellUnlockSource source) {
        this.playerId = playerId;
        this.spellId = spellId;
        this.source = source;
    }

    public UUID playerId() {
        return playerId;
    }

    public String spellId() {
        return spellId;
    }

    public SpellUnlockSource source() {
        return source;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
