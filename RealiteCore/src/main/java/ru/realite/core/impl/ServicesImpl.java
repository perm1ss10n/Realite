package ru.realite.core.impl;

import ru.realite.core.api.Scheduler;
import ru.realite.core.api.Services;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простая реестровая "DI" штука (Service Locator).
 *
 * Идея: Core регистрирует сервисы на старте, модули их запрашивают через require().
 *
 * Важно:
 * - register() по умолчанию ЗАПРЕЩАЕТ перезапись, чтобы не ловить "тихие" баги.
 * - если тебе реально нужно заменить — используй replace().
 */
public final class ServicesImpl implements Services {

    private final Map<Class<?>, Object> registry = new ConcurrentHashMap<>();
    private final Scheduler scheduler;

    public ServicesImpl(Scheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    /**
     * Регистрирует сервис. Перезапись запрещена (бросает исключение).
     */
    @Override
    public <T> void register(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");

        Object prev = registry.putIfAbsent(type, impl);
        if (prev != null) {
            throw new IllegalStateException("Service already registered: " + type.getName()
                    + " (existing=" + prev.getClass().getName() + ", new=" + impl.getClass().getName() + ")");
        }
    }

    /**
     * Регистрирует сервис, если его ещё нет. Возвращает true, если реально зарегистрировали.
     */
    @Override
    public <T> boolean registerIfAbsent(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");
        return registry.putIfAbsent(type, impl) == null;
    }

    /**
     * Явно заменяет сервис (перезапись разрешена).
     * Возвращает предыдущую реализацию или null.
     */
    @Override
    public <T> T replace(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");
        Object prev = registry.put(type, impl);
        return prev == null ? null : type.cast(prev);
    }

    /**
     * Требует сервис (если не зарегистрирован — кидает исключение).
     */
    @Override
    public <T> T require(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object v = registry.get(type);
        if (v == null) throw new IllegalStateException("Service not registered: " + type.getName());
        return type.cast(v);
    }

    /**
     * Получает сервис или null.
     */
    @Override
    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object v = registry.get(type);
        return v == null ? null : type.cast(v);
    }

    /**
     * Проверка наличия сервиса.
     */
    @Override
    public boolean has(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return registry.containsKey(type);
    }

    /**
     * Удаляет сервис. Возвращает удалённое значение или null.
     */
    @Override
    public <T> T unregister(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object removed = registry.remove(type);
        return removed == null ? null : type.cast(removed);
    }

    /**
     * Полная очистка (например, при disable/reload ядра).
     */
    @Override
    public void clear() {
        registry.clear();
    }
}
