package ru.realite.city.model;

import org.bukkit.Location;

import java.util.UUID;

public record ShopPoint(
        String id,
        String plotId,
        String world,
        int x,
        int y,
        int z,
        UUID ownerUuid,
        long createdAt,
        boolean enabled
) {
    public boolean matches(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equals(world)) {
            return false;
        }
        return location.getBlockX() == x
                && location.getBlockY() == y
                && location.getBlockZ() == z;
    }

    public double distanceSquared(Location location) {
        if (location == null || location.getWorld() == null) {
            return Double.MAX_VALUE;
        }
        if (!location.getWorld().getName().equals(world)) {
            return Double.MAX_VALUE;
        }
        double dx = location.getX() - (x + 0.5);
        double dy = location.getY() - (y + 0.5);
        double dz = location.getZ() - (z + 0.5);
        return dx * dx + dy * dy + dz * dz;
    }
}
