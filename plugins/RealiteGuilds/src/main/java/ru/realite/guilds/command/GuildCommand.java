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
import ru.realite.guilds.service.GuildUpgradeService;

public final class GuildCommand implements CommandExecutor {

    private final GuildService service;
    private final GuildMessages messages;
    private final GuildSalaryService salaryService;
    private final GuildChatService chatService;
    private final GuildProgressionService progressionService;
    private final GuildUpgradeService upgradeService;

    public GuildCommand(
            GuildService service,
            GuildMessages messages,
            GuildSalaryService salaryService,
            GuildChatService chatService,
            GuildProgressionService progressionService,
            GuildUpgradeService upgradeService) {
        this.service = service;
        this.messages = messages;
        this.salaryService = salaryService;
        this.chatService = chatService;
        this.progressionService = progressionService;
        this.upgradeService = upgradeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                messages.send(player, "usage.root");
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
            case "upgrades" -> handleUpgrades(player, args);
            case "upgrade" -> handleUpgrade(player, args);

            // /g toggle — админский переключатель гильдийского чата (server-level).
            case "toggle" -> handleToggle(player, args);

            default -> messages.send(player, "usage.root");
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
            messages.send(player, "usage.salary");
            return;
        }
        salaryService.handleSalaryInfo(player);
    }

    private void handleUpgrades(Player player, String[] args) {
        if (args.length != 1) {
            messages.send(player, "usage.upgrades");
            return;
        }
        GuildUpgradeService.UpgradeListResult result = upgradeService.list(player);
        switch (result.status()) {
            case SUCCESS -> {
                messages.send(player, "upgrade.list.header");
                for (GuildUpgradeService.UpgradeEntry entry : result.entries()) {
                    String costText;
                    if (entry.maxed()) {
                        costText = messages.raw("upgrade.list.maxed");
                    } else if (entry.nextCost() <= 0.0d) {
                        costText = messages.raw("upgrade.list.unavailable");
                    } else {
                        costText = formatAmount(entry.nextCost());
                    }
                    String description = entry.description();
                    if (description == null || description.isBlank()) {
                        description = messages.raw("upgrade.list.unavailable");
                    }
                    messages.send(player, "upgrade.list.entry",
                            "id", entry.id(),
                            "name", entry.name(),
                            "level", String.valueOf(entry.level()),
                            "max", String.valueOf(entry.maxLevel()),
                            "cost", costText,
                            "description", description);
                }
            }
            case NOT_IN_GUILD -> messages.send(player, "error.guild.no_member");
            case GUILD_NOT_FOUND -> messages.send(player, "guild.not_found");
            case INVALID_REQUEST -> messages.send(player, "error.no_permission");
        }
    }

    private void handleUpgrade(Player player, String[] args) {
        if (args.length < 2) {
            messages.send(player, "usage.upgrade");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if ("buy".equals(action)) {
            handleUpgradeBuy(player, args);
            return;
        }
        messages.send(player, "usage.upgrade");
    }

    private void handleUpgradeBuy(Player player, String[] args) {
        if (args.length != 3) {
            messages.send(player, "usage.upgrade");
            return;
        }
        GuildUpgradeService.PurchaseResult result = upgradeService.purchase(player, args[2]);
        switch (result.status()) {
            case SUCCESS -> messages.send(player, "upgrade.buy.success");
            case INSUFFICIENT_FUNDS -> messages.send(player, "upgrade.buy.insufficient_funds");
            case MAX_LEVEL -> messages.send(player, "upgrade.buy.max_level");
            case UPGRADE_NOT_FOUND -> messages.send(player, "upgrade.buy.not_found");
            case NOT_IN_GUILD -> messages.send(player, "error.guild.no_member");
            case GUILD_NOT_FOUND -> messages.send(player, "guild.not_found");
            case NO_PERMISSION -> messages.send(player, "error.no_permission");
            case REQUIREMENTS_NOT_MET -> messages.send(player, "upgrade.buy.requirements");
            case UPGRADE_DISABLED, INVALID_COST, INVALID_REQUEST -> messages.send(player, "upgrade.buy.not_found");
        }
    }

    private void handleToggle(Player player, String[] args) {
        if (args.length != 1) {
            messages.send(player, "usage.toggle");
            return;
        }

        if (!chatService.isToggleCommandEnabled()) {
            messages.send(player, "chat.guild.toggle.command_disabled");
            return;
        }

        if (!chatService.canAdminToggle(player)) {
            messages.send(player, "chat.guild.toggle.no_permission");
            return;
        }

        boolean enabled = chatService.toggleEnabled();
        messages.send(player, enabled ? "chat.guild.toggle.enabled" : "chat.guild.toggle.disabled");
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

    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }
}
