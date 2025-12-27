package ru.realite.core.api;

import java.nio.file.Path;

/**
 * Публичная точка входа в ядро.
 */
public interface CoreApi {
    Platform platform();

    Services services();

    Path dataDirectory();
}
