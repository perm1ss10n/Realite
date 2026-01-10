package ru.realite.magic.api;

import java.util.Collection;
import org.bukkit.event.Event;

/**
 * Provides a list of public magic event contracts.
 */
public interface MagicEventsApi {

    /**
     * Returns immutable collection of supported event types.
     */
    Collection<Class<? extends Event>> eventTypes();
}
