package ru.realite.familiars.listener;

import java.util.Optional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.realite.familiars.model.FamiliarType;
import ru.realite.familiars.service.FamiliarEntityData;
import ru.realite.familiars.service.FamiliarService;

public final class FamiliarCombatListener implements Listener {

    private static final int CONTROL_DURATION_TICKS = 40;

    private final FamiliarService service;

    public FamiliarCombatListener(FamiliarService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOwnerCombat(EntityDamageByEntityEvent event) {
        if (service == null || event == null) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            service.recordOwnerCombat(player.getUniqueId());
        }
        Entity damager = event.getDamager();
        if (damager instanceof Player player) {
            service.recordOwnerCombat(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFamiliarDamage(EntityDamageByEntityEvent event) {
        if (service == null || event == null) {
            return;
        }
        Optional<FamiliarEntityData> data = service.getFamiliarEntityData(event.getDamager());
        if (data.isEmpty()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        FamiliarType type = service.getType(data.get().typeId()).orElse(null);
        if (type == null || !"combat".equalsIgnoreCase(type.role())) {
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, CONTROL_DURATION_TICKS, 0, true, false, false));
    }
}
