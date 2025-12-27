package ru.realite.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Services {
    private static final Map<Class<?>, Object> REGISTRY = new ConcurrentHashMap<>();

    private Services() {}

    public static <T> void register(Class<T> type, T impl) {
        REGISTRY.put(type, impl);
    }

    public static <T> T require(Class<T> type) {
        Object v = REGISTRY.get(type);
        if (v == null) throw new IllegalStateException("Service not registered: " + type.getName());
        return type.cast(v);
    }

    public static <T> T get(Class<T> type) {
        Object v = REGISTRY.get(type);
        return v == null ? null : type.cast(v);
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
