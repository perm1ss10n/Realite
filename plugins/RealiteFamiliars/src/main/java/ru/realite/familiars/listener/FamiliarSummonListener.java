package ru.realite.familiars.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.familiars.service.FamiliarEntityData;
import ru.realite.familiars.service.FamiliarService;

import java.util.Optional;

public final class FamiliarSummonListener implements Listener {

    private final FamiliarService service;

    public FamiliarSummonListener(FamiliarService service) {
        this.service = service;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (service == null) {
            return;
        }
        service.handleLogout(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTargetPlayer(EntityTargetLivingEntityEvent event) {
        if (service == null) {
            return;
        }
        if (!(event.getTarget() instanceof Player)) {
            return;
        }
        Entity entity = event.getEntity();
        if (service.isFamiliarEntity(entity)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (service == null) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Entity damager = event.getDamager();
        if (service.isFamiliarEntity(damager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFamiliarDeath(EntityDeathEvent event) {
        if (service == null) {
            return;
        }
        Optional<FamiliarEntityData> data = service.getFamiliarEntityData(event.getEntity());
        if (data.isEmpty()) {
            return;
        }
        service.handleFamiliarDeath(data.get().ownerId(), data.get().typeId());
    }
}
