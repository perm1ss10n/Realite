package ru.realite.familiars.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.FamiliarXpSource;

public final class FamiliarXpListener implements Listener {

    private static final int KILL_XP = 5;
    private static final double XP_RADIUS = 16.0;

    private final FamiliarService service;

    public FamiliarXpListener(FamiliarService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (service == null || event == null || event.getEntity() == null) {
            return;
        }
        if (event.getEntity() instanceof Player) {
            return;
        }
        if (service.isFamiliarEntity(event.getEntity())) {
            return;
        }
        Location location = event.getEntity().getLocation();
        for (Player player : location.getWorld().getNearbyPlayers(location, XP_RADIUS)) {
            service.getSummoned(player.getUniqueId()).ifPresent(instance -> grantXp(player, instance));
        }
    }

    private void grantXp(Player player, FamiliarInstance instance) {
        if (player == null || instance == null) {
            return;
        }
        service.addExperience(player.getUniqueId(), instance.typeId(), KILL_XP, FamiliarXpSource.KILL);
    }
}
