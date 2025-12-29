package ru.realite.city.model;

import org.bukkit.Location;

import java.util.UUID;

public record Plot(
        String id,
        PlotType type,
        String world,
        int x1,
        int y1,
        int z1,
        int x2,
        int y2,
        int z2,
        int price,
        UUID ownerUuid,
        long createdAt
) {
    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return contains(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public boolean contains(String worldName, int x, int y, int z) {
        if (worldName == null || !worldName.equals(world)) {
            return false;
        }
        return x >= x1 && x <= x2
                && y >= y1 && y <= y2
                && z >= z1 && z <= z2;
    }
}
