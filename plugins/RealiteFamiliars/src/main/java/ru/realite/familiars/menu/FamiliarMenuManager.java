package ru.realite.familiars.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.familiars.RealiteFamiliarsPlugin;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.service.FamiliarService;

public final class FamiliarMenuManager {

    private final RealiteFamiliarsPlugin plugin;
    private final FamiliarService service;
    private final Messages messages;

    public FamiliarMenuManager(RealiteFamiliarsPlugin plugin, FamiliarService service, Messages messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    public void openMain(Player player) {
        new FamiliarMainMenu(this).open(player);
    }

    public void openControl(Player player, String typeId) {
        new FamiliarControlMenu(this, typeId).open(player);
    }

    public void runCommand(Player player, String command, Runnable reopen) {
        if (player == null || command == null) {
            return;
        }
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(player, command);
            if (reopen != null && player.isOnline()) {
                reopen.run();
            }
        });
    }

    public FamiliarService service() {
        return service;
    }

    public Messages messages() {
        return messages;
    }
}
