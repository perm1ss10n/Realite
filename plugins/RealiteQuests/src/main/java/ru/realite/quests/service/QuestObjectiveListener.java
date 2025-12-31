package ru.realite.quests.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import ru.realite.core.api.quests.QuestService;

import java.util.function.Supplier;

public final class QuestObjectiveListener implements Listener {

    private final Supplier<QuestService> questServiceSupplier;

    public QuestObjectiveListener(Supplier<QuestService> questServiceSupplier) {
        this.questServiceSupplier = questServiceSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNpcInteract(PlayerInteractEntityEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        Entity entity = event.getRightClicked();
        questService.handleNpcInteract(event.getPlayer(), entity.getUniqueId().toString(), entity.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleKill(killer, event.getEntityType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleLocation(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleBlockPlace(event.getPlayer(), event.getBlockPlaced().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleBlockBreak(event.getPlayer(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleHoldItem(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleHoldItem(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleHoldItem(player);
    }
}
