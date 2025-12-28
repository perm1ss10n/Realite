package ru.realite.core.api;

import java.sql.Connection;

/**
 * Простое хранилище данных.
 */
public interface Storage {
    Connection connection();

    void close();
}
