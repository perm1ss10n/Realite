package ru.realite.city.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.city.gui.GuiService;
import ru.realite.city.i18n.CityMessages;

public final class PlotCommand implements CommandExecutor {

    private static final String PLOT_PERMISSION = "realite.city.plot.use";

    private final GuiService guiService;
    private final CityMessages messages;

    public PlotCommand(GuiService guiService, CityMessages messages) {
        this.guiService = guiService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        if (!player.hasPermission(PLOT_PERMISSION)) {
            messages.send(player, "city.no-permission", "");
            return true;
        }
        if (guiService == null || !guiService.playerGuiEnabled()) {
            messages.send(player, "ui.city.error.disabled", "");
            return true;
        }
        guiService.openPlayerMain(player);
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "city.only-players", "");
            return null;
        }
        return player;
    }
}
