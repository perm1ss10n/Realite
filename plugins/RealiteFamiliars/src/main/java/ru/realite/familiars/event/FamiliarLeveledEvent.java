package ru.realite.familiars.event;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.realite.familiars.model.FamiliarInstance;

public final class FamiliarLeveledEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final FamiliarInstance instance;
    private final int previousLevel;
    private final int newLevel;

    public FamiliarLeveledEvent(Player player, FamiliarInstance instance, int previousLevel, int newLevel) {
        this.player = Objects.requireNonNull(player, "player");
        this.instance = Objects.requireNonNull(instance, "instance");
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Player player() {
        return player;
    }

    public FamiliarInstance instance() {
        return instance;
    }

    public int previousLevel() {
        return previousLevel;
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
