package ru.realite.magic.api.impl;

import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import ru.realite.magic.api.MagicEventsApi;
import ru.realite.magic.api.event.SpellCastAttemptEvent;
import ru.realite.magic.api.event.SpellCastSuccessEvent;
import ru.realite.magic.api.event.SpellSelectedEvent;
import ru.realite.magic.api.event.SpellUnlockedEvent;

public final class MagicEventsApiImpl implements MagicEventsApi {

    private static final List<Class<? extends Event>> EVENTS = List.of(
            SpellCastAttemptEvent.class,
            SpellCastSuccessEvent.class,
            SpellSelectedEvent.class,
            SpellUnlockedEvent.class
    );

    @Override
    public Collection<Class<? extends Event>> eventTypes() {
        return EVENTS;
    }
}
