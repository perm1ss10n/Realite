package ru.realite.core;

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
public final class Services {

    private static final Map<Class<?>, Object> REGISTRY = new ConcurrentHashMap<>();

    private Services() {}

    /**
     * Регистрирует сервис. Перезапись запрещена (бросает исключение).
     */
    public static <T> void register(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");

        Object prev = REGISTRY.putIfAbsent(type, impl);
        if (prev != null) {
            throw new IllegalStateException("Service already registered: " + type.getName()
                    + " (existing=" + prev.getClass().getName() + ", new=" + impl.getClass().getName() + ")");
        }
    }

    /**
     * Регистрирует сервис, если его ещё нет. Возвращает true, если реально зарегистрировали.
     */
    public static <T> boolean registerIfAbsent(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");
        return REGISTRY.putIfAbsent(type, impl) == null;
    }

    /**
     * Явно заменяет сервис (перезапись разрешена).
     * Возвращает предыдущую реализацию или null.
     */
    public static <T> T replace(Class<T> type, T impl) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(impl, "impl");
        Object prev = REGISTRY.put(type, impl);
        return prev == null ? null : type.cast(prev);
    }

    /**
     * Требует сервис (если не зарегистрирован — кидает исключение).
     */
    public static <T> T require(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object v = REGISTRY.get(type);
        if (v == null) throw new IllegalStateException("Service not registered: " + type.getName());
        return type.cast(v);
    }

    /**
     * Получает сервис или null.
     */
    public static <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object v = REGISTRY.get(type);
        return v == null ? null : type.cast(v);
    }

    /**
     * Проверка наличия сервиса.
     */
    public static boolean has(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return REGISTRY.containsKey(type);
    }

    /**
     * Удаляет сервис. Возвращает удалённое значение или null.
     */
    public static <T> T unregister(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object removed = REGISTRY.remove(type);
        return removed == null ? null : type.cast(removed);
    }

    /**
     * Полная очистка (например, при disable/reload ядра).
     */
    public static void clear() {
        REGISTRY.clear();
    }
}
