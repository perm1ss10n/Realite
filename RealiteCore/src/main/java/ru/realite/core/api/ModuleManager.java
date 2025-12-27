package ru.realite.core.api;

import java.util.Collection;

/**
 * Контракт менеджера модулей.
 */
public interface ModuleManager {
    void register(Module module);

    Module get(String id);

    Collection<Module> all();

    void enableAll();

    void disableAll();
}
