package ru.realite.guilds.command;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildService;

public final class GuildCommand implements CommandExecutor {

    private final GuildService service;
    private final GuildMessages messages;

    public GuildCommand(GuildService service, GuildMessages messages) {
        this.service = service;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("error.player_only"));
            return true;
        }
        if (args.length == 0) {
            messages.send(player, "usage.create");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "info" -> handleInfo(player, args);
            case "disband" -> service.disband(player);
            case "invite" -> handleInvite(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> service.leave(player);
            default -> messages.send(player, "usage.create");
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            messages.send(player, "usage.create");
            return;
        }
        String tag = args[1];
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        service.create(player, tag, name);
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length > 2) {
            messages.send(player, "usage.info");
            return;
        }
        String tag = args.length == 2 ? args[1] : null;
        service.info(player, tag);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length != 2) {
            messages.send(player, "usage.invite");
            return;
        }
        service.invite(player, args[1]);
    }

    private void handleJoin(Player player, String[] args) {
        if (args.length != 2) {
            messages.send(player, "usage.join");
            return;
        }
        service.join(player, args[1]);
    }
}
