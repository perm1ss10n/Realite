package ru.realite.classes.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.ClassHudService;

public class PlayerQuitListener implements Listener {

    private final ClassService classService;
    private final ClassHudService hudService;

    public PlayerQuitListener(ClassService classService, ClassHudService hudService) {
        this.classService = classService;
        this.hudService = hudService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var player = e.getPlayer();
        var prof = classService.getProfile(e.getPlayer());
        if (prof != null) {
            classService.save(prof); // или save(player) / save(uuid) — как у тебя сделано
        }
        classService.invalidate(e.getPlayer());
        if (hudService != null) {
            hudService.clearAll(player);
        }
    }
}
