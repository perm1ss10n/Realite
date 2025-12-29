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
                    + "number INTEGER NOT NULL DEFAULT 0,"
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
                    + "created_at INTEGER NOT NULL,"
                    + "rent_paid_until INTEGER NOT NULL DEFAULT 0"
                    + ")");

            ensurePlotNumberColumn(statement);
            ensurePlotRentColumn(statement);

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

            statement.execute("CREATE TABLE IF NOT EXISTS shop_points ("
                    + "id TEXT PRIMARY KEY,"
                    + "plot_id TEXT NOT NULL,"
                    + "world TEXT NOT NULL,"
                    + "x INTEGER NOT NULL,"
                    + "y INTEGER NOT NULL,"
                    + "z INTEGER NOT NULL,"
                    + "owner_uuid TEXT,"
                    + "created_at INTEGER NOT NULL,"
                    + "enabled INTEGER NOT NULL DEFAULT 1,"
                    + "FOREIGN KEY(plot_id) REFERENCES plots(id) ON DELETE CASCADE"
                    + ")");

            statement.execute("CREATE INDEX IF NOT EXISTS idx_shop_points_plot "
                    + "ON shop_points(plot_id)");
        }
    }

    private void ensurePlotNumberColumn(Statement statement) throws SQLException {
        try {
            statement.execute("ALTER TABLE plots ADD COLUMN number INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
        statement.execute("UPDATE plots SET number = rowid WHERE number IS NULL OR number = 0");
    }

    private void ensurePlotRentColumn(Statement statement) throws SQLException {
        try {
            statement.execute("ALTER TABLE plots ADD COLUMN rent_paid_until INTEGER NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message == null || !message.toLowerCase().contains("duplicate column name")) {
                throw e;
            }
        }
        statement.execute("UPDATE plots SET rent_paid_until = 0 WHERE rent_paid_until IS NULL");
    }
}
