package ru.realite.core.impl;

import ru.realite.core.api.Platform;
import ru.realite.core.api.Storage;
import ru.realite.core.api.StorageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class StorageServiceImpl implements StorageService {

    private final Platform platform;
    private final List<Storage> storages = new ArrayList<>();

    StorageServiceImpl(Platform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public Storage openSqlite(Path file) {
        Objects.requireNonNull(file, "file");
        ensureParentExists(file);
        String url = "jdbc:sqlite:" + file.toAbsolutePath();
        try {
            Connection connection = DriverManager.getConnection(url);
            applyPragmas(connection);
            Storage storage = new SqliteStorage(connection, platform);
            storages.add(storage);
            platform.info("Opened SQLite storage: " + file);
            return storage;
        } catch (SQLException e) {
            platform.error("Failed to open SQLite storage: " + file, e);
            throw new IllegalStateException("Failed to open SQLite storage: " + file, e);
        }
    }

    @Override
    public void shutdown() {
        if (storages.isEmpty()) {
            return;
        }
        platform.info("Shutting down storages: " + storages.size());
        for (Storage storage : List.copyOf(storages)) {
            storage.close();
        }
        storages.clear();
    }

    private void applyPragmas(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute("PRAGMA journal_mode = WAL;");
        }
    }

    private void ensureParentExists(Path file) {
        Path parent = file.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (Exception e) {
            platform.error("Failed to create storage directory: " + parent, e);
            throw new IllegalStateException("Failed to create storage directory: " + parent, e);
        }
    }
}
