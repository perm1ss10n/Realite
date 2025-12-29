package ru.realite.city.storage;

import ru.realite.city.model.ShopListing;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShopListingRepository {
    int loadAll() throws SQLException;

    void upsert(ShopListing listing);

    boolean delete(String shopPointId);

    Optional<ShopListing> findByShopPointId(String shopPointId);

    List<ShopListing> findAll();

    List<ShopListing> findByOwner(UUID ownerUuid);
}
