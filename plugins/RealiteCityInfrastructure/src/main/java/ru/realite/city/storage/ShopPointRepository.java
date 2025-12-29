package ru.realite.city.storage;

import ru.realite.city.model.ShopPoint;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ShopPointRepository {
    int loadAll() throws SQLException;

    void upsert(ShopPoint point);

    boolean delete(String id);

    Optional<ShopPoint> findById(String id);

    List<ShopPoint> findAll();

    List<ShopPoint> findByPlot(String plotId);
}
