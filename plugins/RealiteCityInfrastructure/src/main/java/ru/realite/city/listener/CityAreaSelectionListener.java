package ru.realite.city.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.i18n.CityMessages;

public final class CityAreaSelectionListener implements Listener {

    private final CityAreaSelectionService selectionService;
    private final CityMessages messages;

    public CityAreaSelectionListener(CityAreaSelectionService selectionService, CityMessages messages) {
        this.selectionService = selectionService;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!selectionService.isWandEnabled(player.getUniqueId())) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Location location = block.getLocation();
        if (action == Action.LEFT_CLICK_BLOCK) {
            selectionService.setPos1(player.getUniqueId(), location);
            player.sendMessage("pos1 set to " + formatLocation(location));
        } else {
            selectionService.setPos2(player.getUniqueId(), location);
            player.sendMessage("pos2 set to " + formatLocation(location));
        }
        event.setCancelled(true);
    }

    private String formatLocation(Location location) {
        return location.getWorld().getName()
                + " [" + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ() + "]";
    }
}
