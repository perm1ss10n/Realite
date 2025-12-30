package ru.realite.guilds.model;

import org.bukkit.Location;

public record GuildHome(String world, double x, double y, double z, float yaw, float pitch) {

    public static GuildHome fromLocation(Location location) {
        return new GuildHome(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch());
    }
}
