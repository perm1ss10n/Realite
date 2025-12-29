package ru.realite.city.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import ru.realite.city.CityConfig;
import ru.realite.city.model.ShopListing;
import ru.realite.city.model.ShopPoint;

import java.util.Objects;
import java.util.UUID;

public final class ShopMarkerService {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final CityConfig config;
    private final ShopPointService shopPointService;

    public ShopMarkerService(CityConfig config, ShopPointService shopPointService) {
        this.config = config;
        this.shopPointService = shopPointService;
    }

    public void onListingUpdated(ShopPoint point, ShopListing listing) {
        if (!config.shopMarkerEnabled()) {
            removeMarkers(point);
            return;
        }
        if (!config.shopMarkerUpdateOnChange()) {
            return;
        }
        updateMarkers(point, listing);
    }

    public void updateMarkers(ShopPoint point, ShopListing listing) {
        if (point == null || listing == null) {
            return;
        }
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            return;
        }
        String status = listing.open() ? "&aOPEN" : "&cCLOSED";
        Component line1 = format(config.shopMarkerFormatLine1(), listing, status);
        Component line2 = format(config.shopMarkerFormatLine2(), listing, status);

        UUID line1Id = point.markerUuid();
        UUID line2Id = point.markerLine2Uuid();

        ArmorStand line1Stand = resolveStand(world, line1Id);
        if (line1Stand == null) {
            line1Stand = spawnStand(world, markerLocation(world, point, 1.35));
            line1Id = line1Stand.getUniqueId();
        }
        applyStand(line1Stand, line1);

        ArmorStand line2Stand = null;
        if (line2 != null) {
            line2Stand = resolveStand(world, line2Id);
            if (line2Stand == null) {
                line2Stand = spawnStand(world, markerLocation(world, point, 1.05));
                line2Id = line2Stand.getUniqueId();
            }
            applyStand(line2Stand, line2);
        } else if (line2Id != null) {
            removeStand(world, line2Id);
            line2Id = null;
        }

        shopPointService.updateMarkers(point, line1Id, line2Id);
    }

    public void removeMarkers(ShopPoint point) {
        if (point == null) {
            return;
        }
        World world = Bukkit.getWorld(point.world());
        if (world == null) {
            return;
        }
        if (point.markerUuid() != null) {
            removeStand(world, point.markerUuid());
        }
        if (point.markerLine2Uuid() != null) {
            removeStand(world, point.markerLine2Uuid());
        }
        shopPointService.updateMarkers(point, null, null);
    }

    private ArmorStand resolveStand(World world, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Entity entity = world.getEntity(uuid);
        if (entity instanceof ArmorStand stand) {
            return stand;
        }
        return null;
    }

    private void removeStand(World world, UUID uuid) {
        Entity entity = world.getEntity(uuid);
        if (entity != null) {
            entity.remove();
        }
    }

    private ArmorStand spawnStand(World world, Location location) {
        Objects.requireNonNull(location, "location");
        return (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
    }

    private void applyStand(ArmorStand stand, Component name) {
        stand.customName(name);
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSilent(true);
        stand.setCollidable(false);
        stand.setRemoveWhenFarAway(false);
        stand.setSmall(true);
    }

    private Component format(String template, ShopListing listing, String status) {
        if (template == null || template.isBlank()) {
            return null;
        }
        String raw = template
                .replace("{title}", safe(listing.title()))
                .replace("{category}", safe(listing.category()))
                .replace("{status}", status);
        return LEGACY.deserialize(raw);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Location markerLocation(World world, ShopPoint point, double yOffset) {
        return new Location(
                world,
                point.x() + 0.5,
                point.y() + yOffset,
                point.z() + 0.5
        );
    }
}
