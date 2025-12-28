package ru.realite.core.api;

import java.util.Collection;

/**
 * Контракт менеджера модулей.
 */
public interface ModuleManager {
    void register(Module module);

    void loadAll();

    void enableAll();

    void disableAll();

    void enable(ModuleId id);

    void disable(ModuleId id);

    ModuleState state(ModuleId id);

    Collection<Module> modules();
}
