package ru.realite.magic.api.event;

import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public final class SpellCastAttemptEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String spellId;
    private final boolean success;
    @Nullable
    private final String reasonKey;
    private final Map<String, String> placeholders;

    public SpellCastAttemptEvent(UUID playerId,
                                 String spellId,
                                 boolean success,
                                 @Nullable String reasonKey,
                                 @Nullable Map<String, String> placeholders) {
        this.playerId = playerId;
        this.spellId = spellId;
        this.success = success;
        this.reasonKey = reasonKey;
        this.placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }

    public UUID playerId() {
        return playerId;
    }

    public String spellId() {
        return spellId;
    }

    public boolean success() {
        return success;
    }

    public @Nullable String reasonKey() {
        return reasonKey;
    }

    public Map<String, String> placeholders() {
        return placeholders;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
