package ru.realite.city.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.realite.city.model.Plot;
import ru.realite.city.storage.PlotRepository;

import java.util.List;
import java.util.Optional;

public final class CityAdminService {

    private static final String ADMIN_PERMISSION = "realite.city.admin";

    private final CityAreaSelectionService selectionService;
    private final PlotRepository plotRepository;

    public CityAdminService(CityAreaSelectionService selectionService,
                            PlotRepository plotRepository) {
        this.selectionService = selectionService;
        this.plotRepository = plotRepository;
    }

    public AccessResult setSelectionPos1(Player player) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        selectionService.setPos1(player.getUniqueId(), player.getLocation());
        return AccessResult.allow();
    }

    public AccessResult setSelectionPos2(Player player) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        selectionService.setPos2(player.getUniqueId(), player.getLocation());
        return AccessResult.allow();
    }

    public AccessResult clearSelection(Player player) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        selectionService.clearSelection(player.getUniqueId());
        return AccessResult.allow();
    }

    public Optional<CityAreaSelectionService.Selection> selection(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return selectionService.getSelection(player.getUniqueId());
    }

    public List<Plot> listPlots() {
        return plotRepository.findAll();
    }

    public Optional<Plot> findPlot(String plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        return plotRepository.findById(plotId);
    }

    public AccessResult deletePlot(Player player, String plotId) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        if (plotId == null || plotId.isBlank()) {
            return AccessResult.deny("city.plot.not-found");
        }
        if (!plotRepository.delete(plotId)) {
            return AccessResult.deny("city.plot.not-found");
        }
        return AccessResult.allow();
    }

    public AccessResult teleportToPlot(Player player, String plotId) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        if (plotId == null || plotId.isBlank()) {
            return AccessResult.deny("city.plot.not-found");
        }
        Optional<Plot> plotOptional = plotRepository.findById(plotId);
        if (plotOptional.isEmpty()) {
            return AccessResult.deny("city.plot.not-found");
        }
        Plot plot = plotOptional.get();
        World world = Bukkit.getWorld(plot.world());
        if (world == null) {
            return AccessResult.deny("city.plot.goto.invalid-world");
        }
        int centerX = (plot.x1() + plot.x2()) / 2;
        int centerZ = (plot.z1() + plot.z2()) / 2;
        int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
        Location target = new Location(world, centerX + 0.5, y, centerZ + 0.5);
        player.teleport(target);
        return AccessResult.allow();
    }

    public AccessResult stubSetOwner(Player player) {
        AccessResult access = requireAdmin(player);
        if (!access.isAllowed()) {
            return access;
        }
        return AccessResult.deny("ui.city.error.action_failed");
    }

    private AccessResult requireAdmin(Player player) {
        if (player == null || !player.hasPermission(ADMIN_PERMISSION)) {
            return AccessResult.deny("ui.city.error.no_permission");
        }
        return AccessResult.allow();
    }
}
