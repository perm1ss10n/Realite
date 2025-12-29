package ru.realite.city.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.realite.city.service.CityProtectionService;
import ru.realite.city.i18n.CityMessages;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CityProtectionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 1500L;

    private final CityProtectionService protectionService;
    private final CityMessages messages;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public CityProtectionListener(CityProtectionService protectionService, CityMessages messages) {
        this.protectionService = protectionService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protectionService.canModify(event.getPlayer(), event.getBlock().getLocation())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protectionService.canModify(event.getPlayer(), event.getBlock().getLocation())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        if (!protectionService.canModify(event.getPlayer(), event.getClickedBlock().getLocation())) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!protectionService.canModify(player, event.getEntity().getLocation())) {
            deny(player, event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Location location = event.getEntity().getLocation();
        Entity remover = event.getRemover();
        if (remover instanceof Player player) {
            if (!protectionService.canModify(player, location)) {
                deny(player, event);
            }
            return;
        }
        if (protectionService.isInCityArea(location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block clicked = event.getBlockClicked();
        Location location = clicked != null
                ? clicked.getRelative(event.getBlockFace()).getLocation()
                : event.getPlayer().getLocation();
        if (!protectionService.canModify(event.getPlayer(), location)) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block clicked = event.getBlockClicked();
        Location location = clicked != null ? clicked.getLocation() : event.getPlayer().getLocation();
        if (!protectionService.canModify(event.getPlayer(), location)) {
            deny(event.getPlayer(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> protectionService.isInCityArea(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> protectionService.isInCityArea(block.getLocation()));
    }

    private void deny(Player player, org.bukkit.event.Cancellable event) {
        event.setCancelled(true);
        sendProtectedMessage(player);
    }

    private void sendProtectedMessage(Player player) {
        long now = System.currentTimeMillis();
        long last = lastMessageAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastMessageAt.put(player.getUniqueId(), now);
        player.sendMessage(messages.get("city.protected", "&cThis area is protected."));
    }
}
