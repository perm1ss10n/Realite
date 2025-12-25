package ru.realite.classes.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import ru.realite.classes.service.ProgressionService;

public class XpTestListener implements Listener {

    private final ProgressionService progression;

    public XpTestListener(ProgressionService progression) {
        this.progression = progression;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        progression.addXp(killer, 5);
    }
}
