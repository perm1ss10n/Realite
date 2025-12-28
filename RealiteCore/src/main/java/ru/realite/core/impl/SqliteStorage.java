package ru.realite.core.impl;

import ru.realite.core.api.Platform;
import ru.realite.core.api.Storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

final class SqliteStorage implements Storage {

    private final Connection connection;
    private final Platform platform;

    SqliteStorage(Connection connection, Platform platform) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            platform.error("Failed to close SQLite connection", e);
        }
    }
}
