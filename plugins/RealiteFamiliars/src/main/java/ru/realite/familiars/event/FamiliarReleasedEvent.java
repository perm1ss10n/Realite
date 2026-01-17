package ru.realite.familiars.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.realite.familiars.model.FamiliarInstance;

import java.util.Objects;

public final class FamiliarReleasedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final FamiliarInstance instance;

    public FamiliarReleasedEvent(Player player, FamiliarInstance instance) {
        this.player = Objects.requireNonNull(player, "player");
        this.instance = Objects.requireNonNull(instance, "instance");
    }

    public Player player() {
        return player;
    }

    public FamiliarInstance instance() {
        return instance;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
