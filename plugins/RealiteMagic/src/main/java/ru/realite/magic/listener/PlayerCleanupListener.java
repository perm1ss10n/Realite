package ru.realite.magic.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;

public final class PlayerCleanupListener implements Listener {

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;

    public PlayerCleanupListener(MagicService magicService,
                                 PlayerSpellService playerSpellService) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        playerSpellService.flush(playerId);
        playerSpellService.evict(playerId);
        magicService.cleanup(event.getPlayer());
    }
}
