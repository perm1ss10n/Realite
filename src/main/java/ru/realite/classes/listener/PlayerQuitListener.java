package ru.realite.classes.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.classes.service.ClassService;

public class PlayerQuitListener implements Listener {

    private final ClassService classService;

    public PlayerQuitListener(ClassService classService) {
        this.classService = classService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var prof = classService.getProfile(e.getPlayer());
        classService.save(prof);
        classService.invalidate(e.getPlayer());
    }
}
