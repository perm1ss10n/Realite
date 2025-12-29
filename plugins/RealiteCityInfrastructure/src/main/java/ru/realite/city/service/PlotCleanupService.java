package ru.realite.city.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.realite.city.CityConfig;
import ru.realite.city.PlotCleanupMode;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;

import java.util.Map;
import java.util.UUID;

public final class PlotCleanupService {

    private final JavaPlugin plugin;
    private final CityConfig config;
    private final CityMessages messages;
    private final Material fillMaterial;
    private final int clearAboveY;

    public PlotCleanupService(JavaPlugin plugin, CityConfig config, CityMessages messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.fillMaterial = resolveFillMaterial(config.plotCleanupFillBlock());
        this.clearAboveY = config.plotCleanupClearAboveY();
    }

    public boolean cleanupPlot(Plot plot, UUID notifier) {
        if (!config.plotCleanupEnabled()) {
            return false;
        }
        if (plot == null) {
            return false;
        }
        World world = Bukkit.getWorld(plot.world());
        if (world == null) {
            return false;
        }
        int blocksPerTick = Math.max(1, config.plotCleanupBlocksPerTick());
        PlotCleanupTask task = new PlotCleanupTask(
                world,
                plot,
                blocksPerTick,
                config.plotCleanupMode(),
                notifier);
        task.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    private final class PlotCleanupTask extends BukkitRunnable {

        private final World world;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final int baseY;
        private final int blocksPerTick;
        private final PlotCleanupMode mode;
        private final UUID notifier;
        private final String plotId;

        private int currentX;
        private int currentY;
        private int currentZ;

        private PlotCleanupTask(
                World world,
                Plot plot,
                int blocksPerTick,
                PlotCleanupMode mode,
                UUID notifier) {
            this.world = world;
            this.plotId = plot.id();
            this.minX = Math.min(plot.x1(), plot.x2());
            this.maxX = Math.max(plot.x1(), plot.x2());
            this.minY = Math.min(plot.y1(), plot.y2());
            this.maxY = Math.max(plot.y1(), plot.y2());
            this.minZ = Math.min(plot.z1(), plot.z2());
            this.maxZ = Math.max(plot.z1(), plot.z2());
            int baseCandidate = Math.max(minY, world.getMinHeight());
            int configuredBase = clearAboveY >= baseCandidate ? clearAboveY : baseCandidate;
            this.baseY = Math.min(configuredBase, maxY);
            this.blocksPerTick = blocksPerTick;
            this.mode = mode == null ? PlotCleanupMode.AIR_ONLY : mode;
            this.notifier = notifier;
            this.currentX = minX;
            this.currentY = minY;
            this.currentZ = minZ;
        }

        @Override
        public void run() {
            int processed = 0;
            while (processed < blocksPerTick) {
                if (currentY > maxY) {
                    finish();
                    return;
                }
                if (currentY >= baseY) {
                    Material material = materialFor(currentY);
                    if (material != null) {
                        var block = world.getBlockAt(currentX, currentY, currentZ);
                        if (block.getType() != Material.BEDROCK) {
                            block.setType(material, false);
                        }
                    }
                }
                advance();
                processed++;
            }
        }

        private Material materialFor(int y) {
            if (mode == PlotCleanupMode.FLAT) {
                if (y == baseY) {
                    return fillMaterial;
                }
            }
            return Material.AIR;
        }

        private void advance() {
            currentZ++;
            if (currentZ <= maxZ) {
                return;
            }
            currentZ = minZ;
            currentX++;
            if (currentX <= maxX) {
                return;
            }
            currentX = minX;
            currentY++;
        }

        private void finish() {
            cancel();
            if (notifier == null) {
                return;
            }
            Player player = Bukkit.getPlayer(notifier);
            if (player != null) {
                messages.send(player, "city.plot.cleanup.done", "",
                        Map.of("id", plotId));
            }
        }
    }

    private Material resolveFillMaterial(String token) {
        if (token == null || token.isBlank()) {
            return Material.GRASS_BLOCK;
        }
        Material material = Material.matchMaterial(token.toUpperCase());
        return material == null ? Material.GRASS_BLOCK : material;
    }
}
