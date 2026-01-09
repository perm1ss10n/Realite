package ru.realite.magic.listener;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import ru.realite.magic.hud.MagicHudService;
import ru.realite.magic.service.PlayerSpellService;

public final class SpellBarListener implements Listener {

    private final PlayerSpellService playerSpellService;
    private final MagicHudService hudService;

    public SpellBarListener(PlayerSpellService playerSpellService,
                            MagicHudService hudService) {
        this.playerSpellService = playerSpellService;
        this.hudService = hudService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        int direction = scrollDirection(event.getPreviousSlot(), event.getNewSlot());
        if (direction == 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int current = playerSpellService.getActiveSlot(playerId);
        int next = wrapSlot(current + direction);
        playerSpellService.setActiveSlot(playerId, next);
        String spellId = playerSpellService.getActiveSlotSpell(playerId).orElse(null);
        hudService.showSelected(player, next, spellId);
    }

    private int scrollDirection(int previousSlot, int newSlot) {
        int delta = (newSlot - previousSlot + 9) % 9;
        if (delta == 1) {
            return 1;
        }
        if (delta == 8) {
            return -1;
        }
        return 0;
    }

    private int wrapSlot(int slot) {
        if (slot < 1) {
            return 9;
        }
        if (slot > 9) {
            return 1;
        }
        return slot;
    }
}
