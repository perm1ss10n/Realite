package ru.realite.quests.service;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
}
