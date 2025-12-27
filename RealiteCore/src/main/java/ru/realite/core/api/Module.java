package ru.realite.core.api;

import java.util.List;

/**
 * Контракт модуля (Classes/Quests/CityInfra и т.д.)
 */
public interface Module {

    /**
     * Уникальный id модуля, например: "classes", "quests", "city".
     */
    String id();

    /**
     * Список id модулей, от которых зависит этот модуль.
     * Например, quests может зависеть от classes.
     */
    default List<String> dependsOn() {
        return List.of();
    }

    /**
     * Включение модуля.
     */
    void onEnable(CoreApi core) throws Exception;

    /**
     * Выключение модуля (обязательно освобождаем ресурсы/хендлеры/таски).
     */
    void onDisable() throws Exception;
}
