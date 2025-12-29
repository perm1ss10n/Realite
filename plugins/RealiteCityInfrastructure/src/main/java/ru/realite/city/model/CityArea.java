package ru.realite.city.model;

import org.bukkit.Location;

public record CityArea(
        String id,
        String world,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
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
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
