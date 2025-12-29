package ru.realite.city.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketService {

    public enum TeleportStatus {
        SUCCESS,
        DISABLED,
        NO_PERMISSION,
        COOLDOWN,
        NO_ECONOMY,
        NOT_ENOUGH_MONEY,
        INVALID_TARGET
    }

    public record TeleportResult(TeleportStatus status, long cooldownSeconds, double cost) {
    }

    private final CityConfig config;
    private final EconomyService economyService;
    private final CityHooks hooks;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public MarketService(CityConfig config, EconomyService economyService, CityHooks hooks) {
        this.config = config;
        this.economyService = economyService;
        this.hooks = hooks;
    }

    public TeleportResult teleport(Player player, Location target) {
        if (!config.marketTeleportEnabled()) {
            return new TeleportResult(TeleportStatus.DISABLED, 0, 0);
        }
        if (player == null || target == null || target.getWorld() == null) {
            return new TeleportResult(TeleportStatus.INVALID_TARGET, 0, 0);
        }
        if (hooks != null && !hooks.canUseMarketTeleport(player)) {
            return new TeleportResult(TeleportStatus.NO_PERMISSION, 0, 0);
        }
        long now = System.currentTimeMillis();
        long cooldownSeconds = Math.max(0, config.marketTeleportCooldownSeconds());
        if (cooldownSeconds > 0) {
            Long last = cooldowns.get(player.getUniqueId());
            if (last != null) {
                long elapsed = (now - last) / 1000L;
                if (elapsed < cooldownSeconds) {
                    return new TeleportResult(
                            TeleportStatus.COOLDOWN,
                            cooldownSeconds - elapsed,
                            0
                    );
                }
            }
        }
        double baseCost = Math.max(0, config.marketTeleportCost());
        double multiplier = hooks == null ? 1.0 : hooks.marketTeleportCostMultiplier(player);
        double cost = baseCost * multiplier;
        if (cost > 0) {
            if (economyService == null || !economyService.isAvailable()) {
                return new TeleportResult(TeleportStatus.NO_ECONOMY, 0, cost);
            }
            if (!economyService.has(player, cost)) {
                return new TeleportResult(TeleportStatus.NOT_ENOUGH_MONEY, 0, cost);
            }
            if (!economyService.withdraw(player, cost)) {
                return new TeleportResult(TeleportStatus.NOT_ENOUGH_MONEY, 0, cost);
            }
        }
        player.teleport(target);
        cooldowns.put(player.getUniqueId(), now);
        return new TeleportResult(TeleportStatus.SUCCESS, 0, cost);
    }
}
