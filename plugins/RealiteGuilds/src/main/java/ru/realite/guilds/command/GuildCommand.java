package ru.realite.guilds.command;

import java.util.Arrays;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildChatService;
import ru.realite.guilds.service.GuildProgressionService;
import ru.realite.guilds.service.GuildSalaryService;
import ru.realite.guilds.service.GuildService;

public final class GuildCommand implements CommandExecutor {

    private final GuildService service;
    private final GuildMessages messages;
    private final GuildSalaryService salaryService;
    private final GuildChatService chatService;
    private final GuildProgressionService progressionService;

    public GuildCommand(GuildService service, GuildMessages messages, GuildSalaryService salaryService,
                        GuildChatService chatService, GuildProgressionService progressionService) {
        this.service = service;
        this.messages = messages;
        this.salaryService = salaryService;
        this.chatService = chatService;
        this.progressionService = progressionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                messages.send(player, "usage.create");
            } else {
                sender.sendMessage(messages.msg("error.player_only"));
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("admin".equals(sub)) {
            handleAdmin(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("error.player_only"));
            return true;
        }
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "info" -> handleInfo(player, args);
            case "disband" -> service.disband(player);
            case "invite" -> handleInvite(player, args);
            case "join" -> handleJoin(player, args);
            case "leave" -> service.leave(player);
            case "ranks" -> handleRanks(player, args);
            case "setrank" -> handleSetRank(player, args);
            case "sethome" -> service.setHome(player);
            case "home" -> service.teleportHome(player);
            case "tp" -> handleTp(player, args);
            case "claim" -> handleClaim(player, args);
            case "salary" -> handleSalary(player, args);
            case "chat" -> handleChat(player, args);
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

    private void handleRanks(Player player, String[] args) {
        if (args.length != 1) {
            messages.send(player, "usage.ranks");
            return;
        }
        service.listRanks(player);
    }

    private void handleSetRank(Player player, String[] args) {
        if (args.length != 3) {
            messages.send(player, "usage.setrank");
            return;
        }
        service.setRank(player, args[1], args[2]);
    }

    private void handleClaim(Player player, String[] args) {
        if (args.length != 2) {
            messages.send(player, "usage.claim");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "pos1" -> service.setClaimPos(player, true);
            case "pos2" -> service.setClaimPos(player, false);
            case "apply" -> service.applyClaim(player);
            case "clear" -> service.clearClaim(player);
            default -> messages.send(player, "usage.claim");
        }
    }

    private void handleTp(Player player, String[] args) {
        if (args.length != 2) {
            messages.send(player, "usage.tp");
            return;
        }
        service.teleportToMember(player, args[1]);
    }

    private void handleSalary(Player player, String[] args) {
        if (args.length != 1) {
            messages.send(player, "usage.create");
            return;
        }
        salaryService.handleSalaryInfo(player);
    }

    private void handleChat(Player player, String[] args) {
        if (args.length != 2 || !"toggle".equalsIgnoreCase(args[1])) {
            messages.send(player, "usage.chat");
            return;
        }
        chatService.toggle(player);
    }

    private void handleAdmin(CommandSender sender, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("realite.guilds.admin")) {
            if (sender instanceof Player player) {
                messages.send(player, "error.no_permission");
            } else {
                sender.sendMessage(messages.msg("error.no_permission"));
            }
            return;
        }
        if (args.length >= 3 && "salary".equalsIgnoreCase(args[1]) && "run".equalsIgnoreCase(args[2])) {
            salaryService.handleAdminRun(sender);
            return;
        }
        if (args.length >= 4 && "addxp".equalsIgnoreCase(args[1])) {
            handleAdminAddXp(sender, args);
            return;
        }
        sender.sendMessage(messages.msg("admin.addxp.usage"));
    }

    private void handleAdminAddXp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(messages.msg("admin.addxp.usage"));
            return;
        }
        String tag = args[2];
        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(messages.msg("admin.addxp.usage"));
            return;
        }
        String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "";
        progressionService.addXp(sender, tag, amount, reason);
    }
}
