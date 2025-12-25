package ru.realite.classes.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.service.ClassService;

public class PlayerJoinListener implements Listener {

    private final Plugin plugin;
    private final ClassService classService;
    private final ClassSelectMenu menu;

    public PlayerJoinListener(Plugin plugin, ClassService classService, ClassSelectMenu menu) {
        this.plugin = plugin;
        this.classService = classService;
        this.menu = menu;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        var profile = classService.getProfile(p);
        if (profile.hasClass()) return;

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> p.openInventory(menu.create()),
                20L
        );
    }
}
