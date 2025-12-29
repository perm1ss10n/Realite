package ru.realite.city.storage;

import ru.realite.city.model.ShopListing;
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

public final class SqliteShopListingRepository implements ShopListingRepository {

    private final Storage storage;
    private final Map<String, ShopListing> cache = new ConcurrentHashMap<>();

    public SqliteShopListingRepository(Storage storage) {
        this.storage = storage;
    }

    @Override
    public int loadAll() throws SQLException {
        cache.clear();
        Connection connection = storage.connection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT shop_point_id, plot_id, owner_uuid, title, category, description, open, created_at, updated_at "
                        + "FROM shop_listings"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String ownerRaw = rs.getString("owner_uuid");
                    UUID ownerUuid = null;
                    if (ownerRaw != null && !ownerRaw.isBlank()) {
                        ownerUuid = UUID.fromString(ownerRaw);
                    }
                    ShopListing listing = new ShopListing(
                            rs.getString("shop_point_id"),
                            rs.getString("plot_id"),
                            ownerUuid,
                            rs.getString("title"),
                            rs.getString("category"),
                            rs.getString("description"),
                            rs.getInt("open") != 0,
                            rs.getLong("created_at"),
                            rs.getLong("updated_at")
                    );
                    cache.put(listing.shopPointId(), listing);
                    count++;
                }
                return count;
            }
        }
    }

    @Override
    public void upsert(ShopListing listing) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO shop_listings(shop_point_id, plot_id, owner_uuid, title, category, description, open, created_at, updated_at) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(shop_point_id) DO UPDATE SET "
                            + "plot_id = excluded.plot_id, "
                            + "owner_uuid = excluded.owner_uuid, "
                            + "title = excluded.title, "
                            + "category = excluded.category, "
                            + "description = excluded.description, "
                            + "open = excluded.open, "
                            + "created_at = excluded.created_at, "
                            + "updated_at = excluded.updated_at"
            )) {
                statement.setString(1, listing.shopPointId());
                statement.setString(2, listing.plotId());
                statement.setString(3, listing.ownerUuid() != null ? listing.ownerUuid().toString() : null);
                statement.setString(4, listing.title());
                statement.setString(5, listing.category());
                statement.setString(6, listing.description());
                statement.setInt(7, listing.open() ? 1 : 0);
                statement.setLong(8, listing.createdAt());
                statement.setLong(9, listing.updatedAt());
                statement.executeUpdate();
            }
            cache.put(listing.shopPointId(), listing);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert shop listing: " + listing.shopPointId(), e);
        }
    }

    @Override
    public boolean delete(String shopPointId) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM shop_listings WHERE shop_point_id = ?"
            )) {
                statement.setString(1, shopPointId);
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    cache.remove(shopPointId);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete shop listing: " + shopPointId, e);
        }
    }

    @Override
    public Optional<ShopListing> findByShopPointId(String shopPointId) {
        if (shopPointId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(shopPointId));
    }

    @Override
    public List<ShopListing> findAll() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public List<ShopListing> findByOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return List.of();
        }
        List<ShopListing> result = new ArrayList<>();
        for (ShopListing listing : cache.values()) {
            if (ownerUuid.equals(listing.ownerUuid())) {
                result.add(listing);
            }
        }
        return result;
    }
}
