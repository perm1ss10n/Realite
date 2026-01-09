package ru.realite.magic.api;

import java.util.Collection;
import org.bukkit.event.Event;

public interface MagicEventsApi {

    Collection<Class<? extends Event>> eventTypes();
}
