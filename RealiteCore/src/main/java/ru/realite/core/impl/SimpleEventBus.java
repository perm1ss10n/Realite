package ru.realite.core.impl;

import ru.realite.core.api.EventBus;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Subscription;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Простой синхронный EventBus.
 */
public final class SimpleEventBus implements EventBus {

    private final Platform platform;
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> handlers = new ConcurrentHashMap<>();

    public SimpleEventBus(Platform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");

        CopyOnWriteArrayList<Consumer<?>> list =
                handlers.computeIfAbsent(eventType, key -> new CopyOnWriteArrayList<>());
        list.add(handler);

        return () -> {
            list.remove(handler);
            if (list.isEmpty()) {
                handlers.remove(eventType, list);
            }
        };
    }

    @Override
    public void publish(Object event) {
        if (event == null) {
            return;
        }

        for (Map.Entry<Class<?>, CopyOnWriteArrayList<Consumer<?>>> entry : handlers.entrySet()) {
            Class<?> type = entry.getKey();
            if (!type.isInstance(event)) {
                continue;
            }

            for (Consumer<?> handler : entry.getValue()) {
                dispatch(handler, type, event);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatch(Consumer<?> handler, Class<?> type, Object event) {
        try {
            ((Consumer<T>) handler).accept((T) type.cast(event));
        } catch (Throwable t) {
            platform.error("Event handler failed for " + type.getName(), t);
        }
    }
}
