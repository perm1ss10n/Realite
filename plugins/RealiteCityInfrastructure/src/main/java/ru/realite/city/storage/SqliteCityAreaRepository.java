package ru.realite.city.storage;

import org.bukkit.Location;
import ru.realite.city.model.CityArea;
import ru.realite.core.api.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SqliteCityAreaRepository implements CityAreaRepository {

    private final Storage storage;
    private final Map<String, CityArea> cache = new ConcurrentHashMap<>();

    public SqliteCityAreaRepository(Storage storage) {
        this.storage = storage;
    }

    public int loadAll() throws SQLException {
        cache.clear();
        Connection connection = storage.connection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, world, min_x, min_y, min_z, max_x, max_y, max_z, created_at FROM city_areas"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    CityArea area = new CityArea(
                            rs.getString("id"),
                            rs.getString("world"),
                            rs.getInt("min_x"),
                            rs.getInt("min_y"),
                            rs.getInt("min_z"),
                            rs.getInt("max_x"),
                            rs.getInt("max_y"),
                            rs.getInt("max_z"),
                            rs.getLong("created_at")
                    );
                    cache.put(area.id(), area);
                    count++;
                }
                return count;
            }
        }
    }

    @Override
    public void upsert(CityArea area) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO city_areas(id, world, min_x, min_y, min_z, max_x, max_y, max_z, created_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(id) DO UPDATE SET "
                            + "world = excluded.world, "
                            + "min_x = excluded.min_x, "
                            + "min_y = excluded.min_y, "
                            + "min_z = excluded.min_z, "
                            + "max_x = excluded.max_x, "
                            + "max_y = excluded.max_y, "
                            + "max_z = excluded.max_z, "
                            + "created_at = excluded.created_at"
            )) {
                statement.setString(1, area.id());
                statement.setString(2, area.world());
                statement.setInt(3, area.minX());
                statement.setInt(4, area.minY());
                statement.setInt(5, area.minZ());
                statement.setInt(6, area.maxX());
                statement.setInt(7, area.maxY());
                statement.setInt(8, area.maxZ());
                statement.setLong(9, area.createdAt());
                statement.executeUpdate();
            }
            cache.put(area.id(), area);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert city area: " + area.id(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM city_areas WHERE id = ?"
            )) {
                statement.setString(1, id);
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    cache.remove(id);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete city area: " + id, e);
        }
    }

    @Override
    public Optional<CityArea> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<CityArea> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public Optional<CityArea> findContaining(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        for (CityArea area : cache.values()) {
            if (area.contains(location)) {
                return Optional.of(area);
            }
        }
        return Optional.empty();
    }
}
