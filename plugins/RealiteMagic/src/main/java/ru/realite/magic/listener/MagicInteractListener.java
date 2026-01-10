package ru.realite.magic.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.realite.magic.cast.CastAttemptResult;
import ru.realite.magic.service.MagicService;

public final class MagicInteractListener implements Listener {

    private final MagicService magicService;

    public MagicInteractListener(MagicService magicService) {
        this.magicService = magicService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        CastAttemptResult result = magicService.tryCastSelected(event.getPlayer());
        magicService.handleCastResult(event.getPlayer(), result);
    }
}
