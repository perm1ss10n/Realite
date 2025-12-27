package ru.realite.core.api;

/**
 * Контракт для Bukkit-плагинов, предоставляющих модуль.
 */
public interface CoreModuleEntrypoint {

    /**
     * Возвращает модуль, который будет включён ядром.
     */
    Module module();
}
