package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotOwnerType;
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

    public enum AddTrustedResult {
        SUCCESS,
        PLOT_NOT_FOUND,
        NOT_OWNED,
        NOT_OWNER,
        SELF,
        LIMIT_REACHED,
        INVALID_TARGET
    }

    public record AddTrustedOutcome(AddTrustedResult result, String targetDisplay) {
    }

    public enum SetOwnerResult {
        SUCCESS,
        PLOT_NOT_FOUND,
        NO_GUILDS,
        GUILD_NOT_FOUND,
        INVALID_INPUT
    }

    public record SetOwnerOutcome(SetOwnerResult result, String ownerDisplay) {
    }

    private final CityConfig config;
    private final CityAreaRepository cityAreaRepository;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;
    private final EconomyService economyService;
    private final CityHooks hooks;
    private final GuildsApi guildsApi;
    private final Map<String, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();
    private final Map<String, PendingSell> pendingSells = new ConcurrentHashMap<>();

    public PlotService(
            CityConfig config,
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            EconomyService economyService,
            CityHooks hooks,
            GuildsApi guildsApi
    ) {
        this.config = config;
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.economyService = economyService;
        this.hooks = hooks;
        this.guildsApi = guildsApi;
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
        if (plot.ownerId() != null) {
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
        long rentPaidUntil = plot.rentPaidUntil();
        if (plot.type() == PlotType.SHOP && config.shopRentEnabled()) {
            long now = System.currentTimeMillis();
            long periodMillis = config.shopRentPeriodHours() * 3600_000L;
            rentPaidUntil = Math.max(rentPaidUntil, now + periodMillis);
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
                PlotOwnerType.PLAYER,
                player.getUniqueId(),
                plot.createdAt(),
                rentPaidUntil
        );
        plotRepository.upsert(updated);
        return BuyResult.SUCCESS;
    }

    public boolean canModify(Player player, Location location) {
        if (player == null || location == null) {
            return true;
        }
        return checkAccess(player.getUniqueId(), location, Action.MODIFY).isAllowed();
    }

    public boolean canInteract(Player player, Location location) {
        if (player == null || location == null) {
            return true;
        }
        return checkAccess(player.getUniqueId(), location, Action.INTERACT).isAllowed();
    }

    public boolean isProtectedFromExplosions(Location location) {
        if (location == null) {
            return false;
        }
        return !checkAccess(null, location, Action.EXPLOSION).isAllowed();
    }

    public AccessResult checkAccess(UUID playerId, Location location, Action action) {
        if (location == null) {
            return AccessResult.allow();
        }
        Player player = playerId == null ? null : Bukkit.getPlayer(playerId);
        if (player != null) {
            if (player.hasPermission(ADMIN_PERMISSION)) {
                return AccessResult.allow();
            }
            String bypassPermission = config.cityAreaBypassPermission();
            if (bypassPermission != null
                    && !bypassPermission.isBlank()
                    && player.hasPermission(bypassPermission)) {
                return AccessResult.allow();
            }
        }
        Optional<Plot> plotOptional = plotRepository.findContaining(location);
        if (plotOptional.isPresent()) {
            Plot plot = plotOptional.get();
            if (plot.ownerId() == null) {
                return AccessResult.allow();
            }
            if (plot.ownerType() == PlotOwnerType.GUILD) {
                if (playerId == null || guildsApi == null) {
                    return AccessResult.deny("plot.access.denied");
                }
                if (!guildsApi.isMember(plot.ownerId(), playerId)) {
                    return AccessResult.deny("plot.access.denied");
                }
                if (action == Action.MODIFY) {
                    return config.guildAllowBuildForMembers()
                            ? AccessResult.allow()
                            : AccessResult.deny("plot.access.denied");
                }
                if (action == Action.INTERACT) {
                    return config.guildAllowInteractForMembers()
                            ? AccessResult.allow()
                            : AccessResult.deny("plot.access.denied");
                }
                return AccessResult.deny("plot.access.denied");
            }
            if (playerId != null) {
                if (plot.isOwnedByPlayer(playerId)) {
                    return AccessResult.allow();
                }
                if (plotMemberRepository.isMember(plot.id(), playerId)) {
                    return AccessResult.allow();
                }
            }
            if (action == Action.INTERACT && plot.type() != PlotType.SHOP && config.allowInteractOutsideMembers()) {
                return AccessResult.allow();
            }
            return AccessResult.deny("plot.access.denied");
        }
        if (cityAreaRepository.findContaining(location).isPresent()) {
            if (config.cityAreaDefaultDeny()) {
                return AccessResult.deny("city.access.denied");
            }
            return AccessResult.allow();
        }
        if (config.accessDefaultOutsideCityAllow()) {
            return AccessResult.allow();
        }
        return AccessResult.deny("city.access.denied");
    }

    public boolean isLimitReached(Player player, PlotType type) {
        if (player == null || player.hasPermission(LIMITS_BYPASS_PERMISSION)) {
            return false;
        }
        int limit = hooks == null ? config.limitFor(type) : hooks.maxPlots(player, type);
        if (limit <= 0) {
            return false;
        }
        long owned = plotRepository.countOwned(player.getUniqueId(), type);
        return owned >= limit;
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

    public AddTrustedOutcome addTrusted(Player actor, String plotId, String targetName) {
        if (actor == null || plotId == null || plotId.isBlank()) {
            return new AddTrustedOutcome(AddTrustedResult.PLOT_NOT_FOUND, "");
        }
        if (targetName == null || targetName.isBlank()) {
            return new AddTrustedOutcome(AddTrustedResult.INVALID_TARGET, "");
        }
        Optional<Plot> plotOptional = plotRepository.findById(plotId);
        if (plotOptional.isEmpty()) {
            return new AddTrustedOutcome(AddTrustedResult.PLOT_NOT_FOUND, "");
        }
        Plot plot = plotOptional.get();
        if (plot.ownerId() == null) {
            return new AddTrustedOutcome(AddTrustedResult.NOT_OWNED, "");
        }
        boolean isAdmin = actor.hasPermission(ADMIN_PERMISSION);
        if (!isAdmin && !plot.isOwnedByPlayer(actor.getUniqueId())) {
            return new AddTrustedOutcome(AddTrustedResult.NOT_OWNER, "");
        }
        var target = Bukkit.getOfflinePlayer(targetName);
        UUID targetId = target.getUniqueId();
        if (actor.getUniqueId().equals(targetId)) {
            return new AddTrustedOutcome(AddTrustedResult.SELF, "");
        }
        if (config.trustedMax() > 0) {
            long trustedCount = plotMemberRepository.findMembers(plotId).values().stream()
                    .filter(role -> role == ru.realite.city.model.PlotMemberRole.TRUSTED)
                    .count();
            if (trustedCount >= config.trustedMax()) {
                return new AddTrustedOutcome(AddTrustedResult.LIMIT_REACHED, "");
            }
        }
        plotMemberRepository.upsert(plotId, targetId, ru.realite.city.model.PlotMemberRole.TRUSTED);
        String display = target.getName() == null ? targetId.toString() : target.getName();
        return new AddTrustedOutcome(AddTrustedResult.SUCCESS, display);
    }

    public SetOwnerOutcome setOwner(String plotId, PlotOwnerType ownerType, String ownerRef) {
        if (plotId == null || plotId.isBlank()) {
            return new SetOwnerOutcome(SetOwnerResult.PLOT_NOT_FOUND, "");
        }
        if (ownerType == null || ownerRef == null || ownerRef.isBlank()) {
            return new SetOwnerOutcome(SetOwnerResult.INVALID_INPUT, "");
        }
        Optional<Plot> plotOptional = plotRepository.findById(plotId);
        if (plotOptional.isEmpty()) {
            return new SetOwnerOutcome(SetOwnerResult.PLOT_NOT_FOUND, "");
        }
        Plot plot = plotOptional.get();
        UUID ownerId;
        String ownerDisplay;
        if (ownerType == PlotOwnerType.PLAYER) {
            var target = Bukkit.getOfflinePlayer(ownerRef);
            ownerId = target.getUniqueId();
            ownerDisplay = target.getName() == null ? ownerId.toString() : target.getName();
        } else {
            if (guildsApi == null || guildsApi instanceof NoopGuildsApi) {
                return new SetOwnerOutcome(SetOwnerResult.NO_GUILDS, "");
            }
            ownerId = guildsApi.findGuildIdByTag(ownerRef).orElse(null);
            if (ownerId == null) {
                return new SetOwnerOutcome(SetOwnerResult.GUILD_NOT_FOUND, "");
            }
            ownerDisplay = ownerRef.toUpperCase();
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
                ownerType,
                ownerId,
                plot.createdAt(),
                plot.rentPaidUntil());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        clearPendingOffers(plot.id());
        return new SetOwnerOutcome(SetOwnerResult.SUCCESS, ownerDisplay);
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
