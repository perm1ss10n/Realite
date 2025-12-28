package ru.realite.core.api;

import java.nio.file.Path;

/**
 * Сервис хранилищ для модулей.
 */
public interface StorageService {
    Storage openSqlite(Path file);

    void shutdown();
}
