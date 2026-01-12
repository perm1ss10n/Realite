package ru.realite.city.storage;

import org.bukkit.Location;
import ru.realite.city.model.CityArea;

import java.util.List;
import java.util.Optional;

public interface CityAreaRepository {
    void upsert(CityArea area);

    boolean delete(String id);

    Optional<CityArea> findById(String id);

    List<CityArea> findAll();

    Optional<CityArea> findContaining(Location location);
}
