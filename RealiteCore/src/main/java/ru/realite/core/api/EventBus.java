package ru.realite.core.api;

import java.util.function.Consumer;

/**
 * Минимальный EventBus для общения модулей через CoreApi.
 */
public interface EventBus {
    <T> Subscription subscribe(Class<T> eventType, Consumer<T> handler);

    void publish(Object event);
}
