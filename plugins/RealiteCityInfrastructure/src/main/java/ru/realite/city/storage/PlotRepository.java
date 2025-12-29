package ru.realite.city.storage;

import org.bukkit.Location;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlotRepository {
    void upsert(Plot plot);

    boolean delete(String id);

    Optional<Plot> findById(String id);

    Optional<Plot> findByNumber(int number);

    List<Plot> findAll();

    List<Plot> findByOwner(UUID owner);

    long countOwned(UUID owner, PlotType type);

    Optional<Plot> findContaining(Location location);

    int nextNumber();
}
