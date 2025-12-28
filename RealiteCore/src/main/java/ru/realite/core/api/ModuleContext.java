package ru.realite.core.api;

import java.nio.file.Path;

/**
 * Контекст модуля.
 */
public interface ModuleContext {
    CoreApi core();

    Services services();

    EventBus events();

    Scheduler scheduler();

    Platform logger();

    Path dataFolder();
}
