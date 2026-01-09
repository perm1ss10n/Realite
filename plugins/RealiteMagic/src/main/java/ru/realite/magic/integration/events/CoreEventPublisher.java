package ru.realite.magic.integration.events;

import java.util.Objects;
import ru.realite.core.api.EventBus;

public final class CoreEventPublisher implements MagicEventPublisher {

    private final EventBus eventBus;

    public CoreEventPublisher(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    @Override
    public void publish(Object event) {
        eventBus.publish(event);
    }
}
