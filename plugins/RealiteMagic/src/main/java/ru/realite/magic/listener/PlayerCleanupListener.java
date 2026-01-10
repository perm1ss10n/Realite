package ru.realite.magic.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.magic.service.MagicService;

public final class PlayerCleanupListener implements Listener {

    private final MagicService magicService;

    public PlayerCleanupListener(MagicService magicService) {
        this.magicService = magicService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        magicService.cleanup(event.getPlayer());
    }
}
