package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotType;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Optional;
import java.util.UUID;

public final class PlotService {

    private static final String ADMIN_PERMISSION = "realite.city.admin";

    public enum BuyResult {
        SUCCESS,
        NOT_FOUND,
        ALREADY_OWNED,
        TYPE_DISABLED,
        LIMIT_REACHED,
        NO_ECONOMY
    }

    private final CityConfig config;
    private final CityAreaRepository cityAreaRepository;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;

    public PlotService(
            CityConfig config,
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository
    ) {
        this.config = config;
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
    }

    public Optional<Plot> findContaining(Location location) {
        return plotRepository.findContaining(location);
    }

    public boolean isInCityArea(Location location) {
        return cityAreaRepository.findContaining(location).isPresent();
    }

    public BuyResult buyPlot(Player player, String plotId) {
        if (player == null || plotId == null || plotId.isBlank()) {
            return BuyResult.NOT_FOUND;
        }
        Optional<Plot> plotOptional = plotRepository.findById(plotId);
        if (plotOptional.isEmpty()) {
            return BuyResult.NOT_FOUND;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() != null) {
            return BuyResult.ALREADY_OWNED;
        }
        if (!isTypeEnabled(plot.type())) {
            return BuyResult.TYPE_DISABLED;
        }
        long owned = plotRepository.countOwned(player.getUniqueId(), null);
        if (owned >= config.defaultPlotsPerPlayer()) {
            return BuyResult.LIMIT_REACHED;
        }
        if (plot.price() > 0) {
            return BuyResult.NO_ECONOMY;
        }
        Plot updated = new Plot(
                plot.id(),
                plot.type(),
                plot.world(),
                plot.x1(),
                plot.y1(),
                plot.z1(),
                plot.x2(),
                plot.y2(),
                plot.z2(),
                plot.price(),
                player.getUniqueId(),
                plot.createdAt()
        );
        plotRepository.upsert(updated);
        return BuyResult.SUCCESS;
    }

    public boolean canModify(Player player, Location location) {
        if (player == null || location == null) {
            return true;
        }
        if (!config.cityAreaDefaultDeny()) {
            return true;
        }
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        String bypassPermission = config.cityAreaBypassPermission();
        if (bypassPermission != null
                && !bypassPermission.isBlank()
                && player.hasPermission(bypassPermission)) {
            return true;
        }
        if (!isInCityArea(location)) {
            return true;
        }
        Optional<Plot> plotOptional = plotRepository.findContaining(location);
        if (plotOptional.isEmpty()) {
            return false;
        }
        Plot plot = plotOptional.get();
        UUID playerId = player.getUniqueId();
        if (playerId.equals(plot.ownerUuid())) {
            return true;
        }
        return plotMemberRepository.isMember(plot.id(), playerId);
    }

    private boolean isTypeEnabled(PlotType type) {
        if (type == PlotType.HOME) {
            return config.homePlotsEnabled();
        }
        if (type == PlotType.SHOP) {
            return config.shopPlotsEnabled();
        }
        return false;
    }
}
