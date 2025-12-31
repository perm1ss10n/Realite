package ru.realite.city.service;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotBorderVisualizationService {

    private static final long PERIOD_TICKS = 5L;
    private static final double MIN_STEP = 0.5;

    private final JavaPlugin plugin;
    private final CityConfig config;
    private final CityMessages messages;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PlotBorderVisualizationService(JavaPlugin plugin, CityConfig config, CityMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void showBorder(Player player, Plot plot) {
        if (player == null || plot == null) {
            return;
        }
        if (!config.visualBorderEnabled()) {
            messages.send(player, "visual.border.disabled", "");
            return;
        }
        int cooldownSeconds = Math.max(0, config.visualBorderCooldownSeconds());
        long now = System.currentTimeMillis();
        Long lastShown = cooldowns.get(player.getUniqueId());
        if (lastShown != null && cooldownSeconds > 0) {
            long elapsed = now - lastShown;
            long cooldownMillis = cooldownSeconds * 1000L;
            if (elapsed < cooldownMillis) {
                long remainingSeconds = Math.max(1, (cooldownMillis - elapsed + 999) / 1000);
                messages.send(player, "visual.border.cooldown", "",
                        Map.of("seconds", String.valueOf(remainingSeconds)));
                return;
            }
        }
        cooldowns.put(player.getUniqueId(), now);

        World world = Bukkit.getWorld(plot.world());
        if (world == null) {
            return;
        }

        double step = Math.max(MIN_STEP, config.visualBorderStep());
        List<Location> points = borderPoints(plot, world, step, clampY(plot, player));
        int durationSeconds = Math.max(1, config.visualBorderDurationSeconds());
        long totalTicks = durationSeconds * 20L;
        int totalRuns = (int) Math.max(1, totalTicks / PERIOD_TICKS);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(80, 210, 255), 1.2f);

        messages.send(player, "visual.border.shown", "");

        new BukkitRunnable() {
            private int runs;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (runs++ >= totalRuns) {
                    cancel();
                    return;
                }
                for (Location location : points) {
                    player.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, dust);
                }
            }
        }.runTaskTimer(plugin, 0L, PERIOD_TICKS);
    }

    private double clampY(Plot plot, Player player) {
        int minY = Math.min(plot.y1(), plot.y2());
        int maxY = Math.max(plot.y1(), plot.y2());
        double playerY = player.getLocation().getY();
        double clamped = Math.max(minY, Math.min(maxY, playerY));
        return clamped + 0.2;
    }

    private List<Location> borderPoints(Plot plot, World world, double step, double y) {
        int minX = Math.min(plot.x1(), plot.x2());
        int maxX = Math.max(plot.x1(), plot.x2());
        int minZ = Math.min(plot.z1(), plot.z2());
        int maxZ = Math.max(plot.z1(), plot.z2());

        List<Location> points = new ArrayList<>();
        for (double x = minX; x <= maxX; x += step) {
            points.add(new Location(world, x, y, minZ));
            points.add(new Location(world, x, y, maxZ));
        }
        for (double z = minZ; z <= maxZ; z += step) {
            points.add(new Location(world, minX, y, z));
            points.add(new Location(world, maxX, y, z));
        }
        return points;
    }
}
