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
                    + "world TEXT NOT NULL,"
                    + "x1 INTEGER NOT NULL,"
                    + "y1 INTEGER NOT NULL,"
                    + "z1 INTEGER NOT NULL,"
                    + "x2 INTEGER NOT NULL,"
                    + "y2 INTEGER NOT NULL,"
                    + "z2 INTEGER NOT NULL,"
                    + "price INTEGER NOT NULL DEFAULT 0,"
                    + "owner_uuid TEXT,"
                    + "created_at INTEGER NOT NULL"
                    + ")");

            statement.execute("CREATE INDEX IF NOT EXISTS idx_plots_owner "
                    + "ON plots(owner_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_plots_world "
                    + "ON plots(world)");

            statement.execute("CREATE TABLE IF NOT EXISTS plot_members ("
                    + "plot_id TEXT NOT NULL,"
                    + "member_uuid TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "PRIMARY KEY (plot_id, member_uuid),"
                    + "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE"
                    + ")");
        }
    }
}
