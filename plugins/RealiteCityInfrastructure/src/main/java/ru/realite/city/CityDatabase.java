package ru.realite.city;

import ru.realite.core.api.Storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class CityDatabase {

    private final Storage storage;

    public CityDatabase(Storage storage) {
        this.storage = storage;
    }

    public void migrate() throws SQLException {
        Connection connection = storage.connection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS city_areas ("
                    + "id TEXT PRIMARY KEY,"
                    + "world TEXT NOT NULL,"
                    + "min_x INTEGER NOT NULL,"
                    + "min_y INTEGER NOT NULL,"
                    + "min_z INTEGER NOT NULL,"
                    + "max_x INTEGER NOT NULL,"
                    + "max_y INTEGER NOT NULL,"
                    + "max_z INTEGER NOT NULL,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            statement.execute("CREATE TABLE IF NOT EXISTS plots ("
                    + "id TEXT PRIMARY KEY,"
                    + "type TEXT NOT NULL,"
                    + "price INTEGER NOT NULL,"
                    + "world TEXT NOT NULL,"
                    + "min_x INTEGER NOT NULL,"
                    + "min_y INTEGER NOT NULL,"
                    + "min_z INTEGER NOT NULL,"
                    + "max_x INTEGER NOT NULL,"
                    + "max_y INTEGER NOT NULL,"
                    + "max_z INTEGER NOT NULL,"
                    + "owner_uuid TEXT,"
                    + "status TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            statement.execute("CREATE INDEX IF NOT EXISTS idx_plots_bounds "
                    + "ON plots(world, min_x, max_x, min_z, max_z)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_plots_owner_uuid "
                    + "ON plots(owner_uuid)");

            statement.execute("CREATE TABLE IF NOT EXISTS plot_members ("
                    + "plot_id TEXT NOT NULL,"
                    + "member_uuid TEXT NOT NULL,"
                    + "created_at INTEGER NOT NULL,"
                    + "PRIMARY KEY (plot_id, member_uuid),"
                    + "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE"
                    + ")");
        }
    }
}
