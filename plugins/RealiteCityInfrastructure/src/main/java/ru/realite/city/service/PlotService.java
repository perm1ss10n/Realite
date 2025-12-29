package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotType;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotService {

    private static final String ADMIN_PERMISSION = "realite.city.admin";
    private static final String LIMITS_BYPASS_PERMISSION = "realite.city.bypass.limits";

    public enum BuyResult {
        SUCCESS,
        NOT_FOUND,
        ALREADY_OWNED,
        TYPE_DISABLED,
        LIMIT_REACHED,
        NOT_ENOUGH_MONEY,
        NO_ECONOMY
    }

    public record PendingTransfer(UUID targetUuid, long expiresAt) {
    }

    public record PendingSell(UUID targetUuid, long expiresAt, int price) {
    }

    private final CityConfig config;
    private final CityAreaRepository cityAreaRepository;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;
    private final EconomyService economyService;
    private final Map<String, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();
    private final Map<String, PendingSell> pendingSells = new ConcurrentHashMap<>();

    public PlotService(
            CityConfig config,
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            EconomyService economyService
    ) {
        this.config = config;
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.economyService = economyService;
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
        if (isLimitReached(player, plot.type())) {
            return BuyResult.LIMIT_REACHED;
        }
        if (plot.price() > 0) {
            if (economyService == null || !economyService.isAvailable()) {
                return BuyResult.NO_ECONOMY;
            }
            if (!economyService.has(player, plot.price())) {
                return BuyResult.NOT_ENOUGH_MONEY;
            }
            if (!economyService.withdraw(player, plot.price())) {
                return BuyResult.NOT_ENOUGH_MONEY;
            }
        }
        Plot updated = new Plot(
                plot.id(),
                plot.number(),
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
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        String bypassPermission = config.cityAreaBypassPermission();
        if (bypassPermission != null
                && !bypassPermission.isBlank()
                && player.hasPermission(bypassPermission)) {
            return true;
        }
        Optional<Plot> plotOptional = plotRepository.findContaining(location);
        if (plotOptional.isPresent()) {
            Plot plot = plotOptional.get();
            if (plot.ownerUuid() == null) {
                return true;
            }
            UUID playerId = player.getUniqueId();
            if (playerId.equals(plot.ownerUuid())) {
                return true;
            }
            return plotMemberRepository.isMember(plot.id(), playerId);
        }
        if (!config.cityAreaDefaultDeny()) {
            return true;
        }
        return !isInCityArea(location);
    }

    public boolean canInteract(Player player, Location location) {
        if (player == null || location == null) {
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
        Optional<Plot> plotOptional = plotRepository.findContaining(location);
        if (plotOptional.isPresent()) {
            Plot plot = plotOptional.get();
            if (plot.ownerUuid() == null) {
                return true;
            }
            UUID playerId = player.getUniqueId();
            if (playerId.equals(plot.ownerUuid())) {
                return true;
            }
            if (plotMemberRepository.isMember(plot.id(), playerId)) {
                return true;
            }
            return config.allowInteractOutsideMembers();
        }
        if (!config.cityAreaDefaultDeny()) {
            return true;
        }
        return !isInCityArea(location);
    }

    public boolean isProtectedFromExplosions(Location location) {
        if (location == null) {
            return false;
        }
        Optional<Plot> plotOptional = plotRepository.findContaining(location);
        if (plotOptional.isPresent()) {
            return plotOptional.get().ownerUuid() != null;
        }
        return config.cityAreaDefaultDeny() && isInCityArea(location);
    }

    public boolean isLimitReached(Player player, PlotType type) {
        if (player == null || player.hasPermission(LIMITS_BYPASS_PERMISSION)) {
            return false;
        }
        long owned = plotRepository.countOwned(player.getUniqueId(), type);
        return owned >= config.limitFor(type);
    }

    public void createTransferOffer(String plotId, UUID targetUuid, long expiresAt) {
        if (plotId == null || targetUuid == null) {
            return;
        }
        pendingSells.remove(plotId);
        pendingTransfers.put(plotId, new PendingTransfer(targetUuid, expiresAt));
    }

    public void createSellOffer(String plotId, UUID targetUuid, long expiresAt, int price) {
        if (plotId == null || targetUuid == null) {
            return;
        }
        pendingTransfers.remove(plotId);
        pendingSells.put(plotId, new PendingSell(targetUuid, expiresAt, price));
    }

    public Optional<PendingTransfer> getPendingTransfer(String plotId) {
        PendingTransfer pending = pendingTransfers.get(plotId);
        if (pending == null) {
            return Optional.empty();
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            pendingTransfers.remove(plotId);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public Optional<PendingSell> getPendingSell(String plotId) {
        PendingSell pending = pendingSells.get(plotId);
        if (pending == null) {
            return Optional.empty();
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            pendingSells.remove(plotId);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public void clearPendingOffers(String plotId) {
        if (plotId == null) {
            return;
        }
        pendingTransfers.remove(plotId);
        pendingSells.remove(plotId);
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
