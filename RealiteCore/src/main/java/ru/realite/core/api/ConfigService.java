package ru.realite.core.api;

import java.nio.file.Path;

/**
 * Сервис работы с конфигами.
 */
public interface ConfigService {
    Config load(Path file);

    Config loadOrCreateDefault(Path file, String resourcePath, ClassLoader cl);
}
