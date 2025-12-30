package ru.realite.guilds.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.realite.guilds.service.GuildSalaryService;

public final class GuildSalaryJoinListener implements Listener {

    private final GuildSalaryService salaryService;

    public GuildSalaryJoinListener(GuildSalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        salaryService.handlePlayerJoin(event.getPlayer());
    }
}
