package ru.realite.core.api;

/**
 * Контракт реестра сервисов.
 */
public interface Services {
    Scheduler scheduler();

    <T> void register(Class<T> type, T impl);

    <T> boolean registerIfAbsent(Class<T> type, T impl);

    <T> T replace(Class<T> type, T impl);

    <T> T require(Class<T> type);

    <T> T get(Class<T> type);

    boolean has(Class<?> type);

    <T> T unregister(Class<T> type);

    void clear();
}
