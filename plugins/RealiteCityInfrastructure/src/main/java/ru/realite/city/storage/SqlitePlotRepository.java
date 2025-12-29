package ru.realite.city.storage;

import org.bukkit.Location;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotType;
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

public final class SqlitePlotRepository implements PlotRepository {

    private final Storage storage;
    private final Map<String, Plot> cache = new ConcurrentHashMap<>();

    public SqlitePlotRepository(Storage storage) {
        this.storage = storage;
    }

    public int loadAll() throws SQLException {
        cache.clear();
        Connection connection = storage.connection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, number, type, world, x1, y1, z1, x2, y2, z2, price, owner_uuid, created_at FROM plots"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    UUID ownerUuid = null;
                    String ownerRaw = rs.getString("owner_uuid");
                    if (ownerRaw != null && !ownerRaw.isBlank()) {
                        ownerUuid = UUID.fromString(ownerRaw);
                    }
                    Plot plot = new Plot(
                            rs.getString("id"),
                            rs.getInt("number"),
                            PlotType.valueOf(rs.getString("type")),
                            rs.getString("world"),
                            rs.getInt("x1"),
                            rs.getInt("y1"),
                            rs.getInt("z1"),
                            rs.getInt("x2"),
                            rs.getInt("y2"),
                            rs.getInt("z2"),
                            rs.getInt("price"),
                            ownerUuid,
                            rs.getLong("created_at")
                    );
                    cache.put(plot.id(), plot);
                    count++;
                }
                return count;
            }
        }
    }

    @Override
    public void upsert(Plot plot) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO plots(id, number, type, world, x1, y1, z1, x2, y2, z2, price, owner_uuid, created_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(id) DO UPDATE SET "
                            + "number = excluded.number, "
                            + "type = excluded.type, "
                            + "world = excluded.world, "
                            + "x1 = excluded.x1, "
                            + "y1 = excluded.y1, "
                            + "z1 = excluded.z1, "
                            + "x2 = excluded.x2, "
                            + "y2 = excluded.y2, "
                            + "z2 = excluded.z2, "
                            + "price = excluded.price, "
                            + "owner_uuid = excluded.owner_uuid, "
                            + "created_at = excluded.created_at"
            )) {
                statement.setString(1, plot.id());
                statement.setInt(2, plot.number());
                statement.setString(3, plot.type().name());
                statement.setString(4, plot.world());
                statement.setInt(5, plot.x1());
                statement.setInt(6, plot.y1());
                statement.setInt(7, plot.z1());
                statement.setInt(8, plot.x2());
                statement.setInt(9, plot.y2());
                statement.setInt(10, plot.z2());
                statement.setInt(11, plot.price());
                statement.setString(12, plot.ownerUuid() != null ? plot.ownerUuid().toString() : null);
                statement.setLong(13, plot.createdAt());
                statement.executeUpdate();
            }
            cache.put(plot.id(), plot);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert plot: " + plot.id(), e);
        }
    }

    @Override
    public boolean delete(String id) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM plots WHERE id = ?"
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
            throw new IllegalStateException("Failed to delete plot: " + id, e);
        }
    }

    @Override
    public Optional<Plot> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(id));
    }

    @Override
    public Optional<Plot> findByNumber(int number) {
        if (number <= 0) {
            return Optional.empty();
        }
        for (Plot plot : cache.values()) {
            if (plot.number() == number) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Plot> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<Plot> findByOwner(UUID owner) {
        if (owner == null) {
            return List.of();
        }
        List<Plot> result = new ArrayList<>();
        for (Plot plot : cache.values()) {
            if (owner.equals(plot.ownerUuid())) {
                result.add(plot);
            }
        }
        return result;
    }

    @Override
    public long countOwned(UUID owner, PlotType type) {
        if (owner == null) {
            return 0;
        }
        long count = 0;
        for (Plot plot : cache.values()) {
            if (!owner.equals(plot.ownerUuid())) {
                continue;
            }
            if (type != null && plot.type() != type) {
                continue;
            }
            count++;
        }
        return count;
    }

    @Override
    public Optional<Plot> findContaining(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        for (Plot plot : cache.values()) {
            if (plot.contains(location)) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    @Override
    public int nextNumber() {
        int max = 0;
        for (Plot plot : cache.values()) {
            if (plot.number() > max) {
                max = plot.number();
            }
        }
        return max + 1;
    }
}
