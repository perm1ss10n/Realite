package ru.realite.core.api;

import java.util.Collection;

/**
 * Источник модулей для автозагрузки через ServiceLoader.
 */
public interface ModuleProvider {
    Collection<Module> createModules(CoreApi core);
}
