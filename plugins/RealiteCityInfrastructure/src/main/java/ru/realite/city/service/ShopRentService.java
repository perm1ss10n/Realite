package ru.realite.city.service;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotType;
import ru.realite.city.storage.PlotRepository;

import java.util.Map;
import java.util.UUID;

public final class ShopRentService {

    private final JavaPlugin plugin;
    private final CityConfig config;
    private final CityMessages messages;
    private final PlotRepository plotRepository;
    private final ShopPointService shopPointService;
    private final EconomyService economyService;

    private BukkitRunnable task;

    public ShopRentService(
            JavaPlugin plugin,
            CityConfig config,
            CityMessages messages,
            PlotRepository plotRepository,
            ShopPointService shopPointService,
            EconomyService economyService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.plotRepository = plotRepository;
        this.shopPointService = shopPointService;
        this.economyService = economyService;
    }

    public void start() {
        if (!config.shopRentEnabled()) {
            return;
        }
        if (task != null) {
            return;
        }
        long periodTicks = Math.max(1, config.shopRentCheckMinutes()) * 60L * 20L;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                checkPlots();
            }
        };
        task.runTaskTimer(plugin, periodTicks, periodTicks);
    }

    public boolean isRentEnabled() {
        return config.shopRentEnabled();
    }

    public boolean isEconomyAvailable() {
        return economyService != null && economyService.isAvailable();
    }

    public long periodMillis() {
        return config.shopRentPeriodHours() * 3600_000L;
    }

    public long graceMillis() {
        return config.shopRentGraceHours() * 3600_000L;
    }

    public boolean isRentOverdue(Plot plot, long now) {
        return plot.rentPaidUntil() <= now;
    }

    public boolean isCommerceBlocked(Plot plot, long now) {
        return plot.rentPaidUntil() + graceMillis() <= now;
    }

    public Plot applyPayment(Plot plot, int periods) {
        long now = System.currentTimeMillis();
        long base = Math.max(plot.rentPaidUntil(), now);
        long newPaidUntil = base + periodMillis() * Math.max(1, periods);
        return new Plot(
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
                plot.ownerUuid(),
                plot.createdAt(),
                newPaidUntil
        );
    }

    public void onPaymentApplied(String plotId) {
        shopPointService.setEnabledForPlot(plotId, true);
    }

    private void checkPlots() {
        long now = System.currentTimeMillis();
        for (Plot plot : plotRepository.findAll()) {
            if (plot.type() != PlotType.SHOP) {
                continue;
            }
            UUID ownerUuid = plot.ownerUuid();
            if (ownerUuid == null) {
                continue;
            }
            if (!isRentOverdue(plot, now)) {
                continue;
            }
            OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
            Player online = owner.getPlayer();
            if (online != null) {
                messages.send(online, "city.shop.rent.overdue", "",
                        Map.of("id", plot.id()));
            }
            if (isCommerceBlocked(plot, now)) {
                shopPointService.setEnabledForPlot(plot.id(), false);
                if (online != null) {
                    messages.send(online, "city.shop.rent.blocked", "",
                            Map.of("id", plot.id()));
                }
            }
        }
    }
}
