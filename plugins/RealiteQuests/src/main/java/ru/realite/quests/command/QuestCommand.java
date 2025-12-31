package ru.realite.quests.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.core.api.quests.QuestService;
import ru.realite.quests.service.QuestServiceImpl;

import java.util.List;
import java.util.function.Supplier;

public final class QuestCommand implements CommandExecutor {

    private final Supplier<QuestService> questServiceSupplier;

    public QuestCommand(Supplier<QuestService> questServiceSupplier) {
        this.questServiceSupplier = questServiceSupplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest <start|active>");
            return true;
        }
        String sub = args[0].toLowerCase();
        QuestService questService = questServiceSupplier.get();
        if (questService == null) {
            sender.sendMessage(ChatColor.RED + "Quest service not available.");
            return true;
        }
        return switch (sub) {
            case "start" -> handleStart(sender, questService, args);
            case "active" -> handleActive(sender, questService);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                yield true;
            }
        };
    }

    private boolean handleStart(CommandSender sender, QuestService questService, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can start quests.");
            return true;
        }
        if (!sender.hasPermission("realite.quests.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /quest start <id>");
            return true;
        }
        String questId = args[1];
        questService.start(player, questId);
        sender.sendMessage(ChatColor.GREEN + "Quest started: " + questId);
        return true;
    }

    private boolean handleActive(CommandSender sender, QuestService questService) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can view active quests.");
            return true;
        }
        if (questService instanceof QuestServiceImpl questServiceImpl) {
            List<String> active = questServiceImpl.getActiveQuestIds(player);
            if (active.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No active quests.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Active quests: " + String.join(", ", active));
            return true;
        }
        sender.sendMessage(ChatColor.RED + "Quest service not ready.");
        return true;
    }
}
