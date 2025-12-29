package ru.realite.city.service;

import org.bukkit.Location;
import ru.realite.city.model.Plot;
import ru.realite.city.model.ShopPoint;
import ru.realite.city.storage.ShopPointRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ShopPointService {

    private final ShopPointRepository repository;

    public ShopPointService(ShopPointRepository repository) {
        this.repository = repository;
    }

    public ShopPoint create(Plot plot, Location location, UUID ownerUuid) {
        String id = UUID.randomUUID().toString();
        ShopPoint point = new ShopPoint(
                id,
                plot.id(),
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                ownerUuid,
                null,
                null,
                System.currentTimeMillis(),
                true
        );
        repository.upsert(point);
        return point;
    }

    public boolean remove(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return repository.delete(id);
    }

    public Optional<ShopPoint> findById(String id) {
        return repository.findById(id);
    }

    public List<ShopPoint> listByPlot(String plotId) {
        return repository.findByPlot(plotId);
    }

    public List<ShopPoint> listAll() {
        return repository.findAll();
    }

    public Optional<ShopPoint> findExact(Location location) {
        if (location == null) {
            return Optional.empty();
        }
        for (ShopPoint point : repository.findAll()) {
            if (point.matches(location)) {
                return Optional.of(point);
            }
        }
        return Optional.empty();
    }

    public Optional<ShopPoint> findNearest(Location location, double radius) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        double maxDistance = radius * radius;
        return repository.findAll().stream()
                .filter(point -> point.distanceSquared(location) <= maxDistance)
                .min(Comparator.comparingDouble(point -> point.distanceSquared(location)));
    }

    public boolean isShopPoint(Location location) {
        return findExact(location).isPresent();
    }

    public int countByPlot(String plotId) {
        return repository.findByPlot(plotId).size();
    }

    public void setEnabled(ShopPoint point, boolean enabled) {
        if (point == null) {
            return;
        }
        ShopPoint updated = new ShopPoint(
                point.id(),
                point.plotId(),
                point.world(),
                point.x(),
                point.y(),
                point.z(),
                point.ownerUuid(),
                point.markerUuid(),
                point.markerLine2Uuid(),
                point.createdAt(),
                enabled
        );
        repository.upsert(updated);
    }

    public void updateMarkers(ShopPoint point, UUID markerUuid, UUID markerLine2Uuid) {
        if (point == null) {
            return;
        }
        ShopPoint updated = new ShopPoint(
                point.id(),
                point.plotId(),
                point.world(),
                point.x(),
                point.y(),
                point.z(),
                point.ownerUuid(),
                markerUuid,
                markerLine2Uuid,
                point.createdAt(),
                point.enabled()
        );
        repository.upsert(updated);
    }

    public void setEnabledForPlot(String plotId, boolean enabled) {
        if (plotId == null) {
            return;
        }
        for (ShopPoint point : repository.findByPlot(plotId)) {
            setEnabled(point, enabled);
        }
    }
}
