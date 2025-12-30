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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.Material;
import org.bukkit.Tag;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.service.AccessResult;
import ru.realite.city.service.Action;
import ru.realite.city.service.PlotService;
import ru.realite.city.service.ShopPointService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CityProtectionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 1500L;

    private final PlotService plotService;
    private final CityMessages messages;
    private final ShopPointService shopPointService;
    private final Map<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public CityProtectionListener(PlotService plotService, CityMessages messages, ShopPointService shopPointService) {
        this.plotService = plotService;
        this.messages = messages;
        this.shopPointService = shopPointService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        AccessResult result = plotService.checkAccess(
                event.getPlayer().getUniqueId(),
                event.getBlock().getLocation(),
                Action.MODIFY);
        if (!result.isAllowed()) {
            deny(event.getPlayer(), event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        AccessResult result = plotService.checkAccess(
                event.getPlayer().getUniqueId(),
                event.getBlock().getLocation(),
                Action.MODIFY);
        if (!result.isAllowed()) {
            deny(event.getPlayer(), event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (shopPointService != null && shopPointService.isShopPoint(block.getLocation())) {
            return;
        }
        if (!isProtectedInteract(block)) {
            return;
        }
        AccessResult result = plotService.checkAccess(
                event.getPlayer().getUniqueId(),
                block.getLocation(),
                Action.INTERACT);
        if (!result.isAllowed()) {
            deny(event.getPlayer(), event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        AccessResult result = plotService.checkAccess(
                player.getUniqueId(),
                event.getEntity().getLocation(),
                Action.MODIFY);
        if (!result.isAllowed()) {
            deny(player, event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Location location = event.getEntity().getLocation();
        Entity remover = event.getRemover();
        if (remover instanceof Player player) {
            AccessResult result = plotService.checkAccess(player.getUniqueId(), location, Action.MODIFY);
            if (!result.isAllowed()) {
                deny(player, event, result);
            }
            return;
        }
        if (!plotService.checkAccess(null, location, Action.EXPLOSION).isAllowed()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block clicked = event.getBlockClicked();
        Location location = clicked != null
                ? clicked.getRelative(event.getBlockFace()).getLocation()
                : event.getPlayer().getLocation();
        AccessResult result = plotService.checkAccess(event.getPlayer().getUniqueId(), location, Action.MODIFY);
        if (!result.isAllowed()) {
            deny(event.getPlayer(), event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block clicked = event.getBlockClicked();
        Location location = clicked != null ? clicked.getLocation() : event.getPlayer().getLocation();
        AccessResult result = plotService.checkAccess(event.getPlayer().getUniqueId(), location, Action.MODIFY);
        if (!result.isAllowed()) {
            deny(event.getPlayer(), event, result);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block ->
                !plotService.checkAccess(null, block.getLocation(), Action.EXPLOSION).isAllowed());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block ->
                !plotService.checkAccess(null, block.getLocation(), Action.EXPLOSION).isAllowed());
    }

    private void deny(Player player, org.bukkit.event.Cancellable event, AccessResult result) {
        event.setCancelled(true);
        sendProtectedMessage(player, result);
    }

    private void sendProtectedMessage(Player player, AccessResult result) {
        long now = System.currentTimeMillis();
        long last = lastMessageAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastMessageAt.put(player.getUniqueId(), now);
        String key = result.reasonKey() == null ? "city.no-permission" : result.reasonKey();
        messages.send(player, key, "");
    }

    private boolean isProtectedInteract(Block block) {
        if (block == null) {
            return false;
        }
        Material type = block.getType();
        if (block.getState() instanceof InventoryHolder) {
            return true;
        }
        if (type == Material.LEVER) {
            return true;
        }
        if (Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type)) {
            return true;
        }
        return Tag.BUTTONS.isTagged(type);
    }
}
