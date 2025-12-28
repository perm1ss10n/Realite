package ru.realite.core.api;

/**
 * Контракт модуля (Classes/Quests/CityInfra и т.д.)
 */
public interface Module {

    /**
     * Метаданные модуля.
     */
    ModuleMetadata metadata();

    /**
     * Загрузка модуля.
     */
    void onLoad(ModuleContext ctx) throws Exception;

    /**
     * Включение модуля.
     */
    void onEnable(ModuleContext ctx) throws Exception;

    /**
     * Выключение модуля (обязательно освобождаем ресурсы/хендлеры/таски).
     */
    void onDisable(ModuleContext ctx) throws Exception;
}
