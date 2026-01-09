package ru.realite.magic.integration.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public final class BukkitEventPublisher implements MagicEventPublisher {

    @Override
    public void publish(Object event) {
        if (event instanceof Event bukkitEvent) {
            Bukkit.getPluginManager().callEvent(bukkitEvent);
        }
    }
}
