package ru.realite.magic.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import ru.realite.magic.service.MagicService;

public final class CombatListener implements Listener {

    private final MagicService magicService;

    public CombatListener(MagicService magicService) {
        this.magicService = magicService;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player) {
            magicService.markCombat(player);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            magicService.markCombat(player);
        }
    }
}
