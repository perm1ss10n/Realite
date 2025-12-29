package ru.realite.city.storage;

import ru.realite.city.model.ShopPoint;
import ru.realite.core.api.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SqliteShopPointRepository implements ShopPointRepository {

    private final Storage storage;
    private final Map<String, ShopPoint> cache = new ConcurrentHashMap<>();

    public SqliteShopPointRepository(Storage storage) {
        this.storage = storage;
    }

    @Override
    public int loadAll() throws SQLException {
        cache.clear();
        Connection connection = storage.connection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, plot_id, world, x, y, z, owner_uuid, created_at, enabled FROM shop_points"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    UUID ownerUuid = null;
                    String ownerRaw = rs.getString("owner_uuid");
                    if (ownerRaw != null && !ownerRaw.isBlank()) {
                        ownerUuid = UUID.fromString(ownerRaw);
                    }
                    ShopPoint point = new ShopPoint(
                            rs.getString("id"),
                            rs.getString("plot_id"),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            ownerUuid,
                            rs.getLong("created_at"),
                            rs.getInt("enabled") != 0
                    );
                    cache.put(point.id(), point);
                    count++;
                }
                return count;
            }
        }
    }

    @Override
    public void upsert(ShopPoint point) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO shop_points(id, plot_id, world, x, y, z, owner_uuid, created_at, enabled) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(id) DO UPDATE SET "
                            + "plot_id = excluded.plot_id, "
                            + "world = excluded.world, "
                            + "x = excluded.x, "
                            + "y = excluded.y, "
                            + "z = excluded.z, "
                            + "owner_uuid = excluded.owner_uuid, "
                            + "created_at = excluded.created_at, "
                            + "enabled = excluded.enabled"
            )) {
                statement.setString(1, point.id());
                statement.setString(2, point.plotId());
                statement.setString(3, point.world());
                statement.setInt(4, point.x());
                statement.setInt(5, point.y());
                statement.setInt(6, point.z());
                statement.setString(7, point.ownerUuid() != null ? point.ownerUuid().toString() : null);
                statement.setLong(8, point.createdAt());
                statement.setInt(9, point.enabled() ? 1 : 0);
                statement.executeUpdate();
            }
            cache.put(point.id(), point);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert shop point: " + point.id(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM shop_points WHERE id = ?"
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
            throw new IllegalStateException("Failed to delete shop point: " + id, e);
        }
    }

    @Override
    public Optional<ShopPoint> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public List<ShopPoint> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<ShopPoint> findByPlot(String plotId) {
        if (plotId == null) {
            return List.of();
        }
        List<ShopPoint> result = new ArrayList<>();
        for (ShopPoint point : cache.values()) {
            if (plotId.equals(point.plotId())) {
                result.add(point);
            }
        }
        return result;
    }
}
