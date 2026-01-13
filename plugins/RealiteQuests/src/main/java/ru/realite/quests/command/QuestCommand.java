package ru.realite.quests.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.core.api.quests.QuestProgress;
import ru.realite.core.api.quests.QuestService;
import ru.realite.core.api.quests.QuestStartTrigger;
import ru.realite.quests.gui.QuestMenuState;
import ru.realite.quests.gui.QuestsHubMenu;
import ru.realite.quests.model.ObjectiveDefinition;
import ru.realite.quests.model.QuestDefinition;
import ru.realite.quests.service.ConditionCheckResult;
import ru.realite.quests.service.QuestProgressData;
import ru.realite.quests.service.QuestServiceImpl;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class QuestCommand implements CommandExecutor {

    private final Supplier<QuestService> questServiceSupplier;

    public QuestCommand(Supplier<QuestService> questServiceSupplier) {
        this.questServiceSupplier = questServiceSupplier;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        QuestService questService = questServiceSupplier.get();
        if (questService == null) {
            sender.sendMessage(c("Quest service not available.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            return handleMenu(sender, questService);
        }

        String sub = args[0].toLowerCase();

        return switch (sub) {
            case "menu", "gui" -> handleMenu(sender, questService);
            case "start" -> handleStart(sender, questService, args);
            case "active" -> handleActive(sender, questService);
            case "debug" -> handleDebug(sender, questService, args);
            case "reload" -> handleReload(sender, questService);
            default -> {
                sender.sendMessage(c("Usage: /quest [menu|start|active|debug|reload]", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean handleStart(CommandSender sender, QuestService questService, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("Only players can start quests.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("realite.quests.admin") && !sender.isOp()) {
            sender.sendMessage(c("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(c("Usage: /quest start <id> [force]", NamedTextColor.RED));
            return true;
        }

        String questId = args[1];
        boolean force = args.length > 2 && args[2].equalsIgnoreCase("force");
        QuestStartTrigger trigger = force ? QuestStartTrigger.MANUAL : QuestStartTrigger.COMMAND;

        questService.start(player, questId, trigger, force);
        sender.sendMessage(Component.text("Quest started: ", NamedTextColor.GREEN)
                .append(Component.text(questId, NamedTextColor.GOLD)));
        return true;
    }

    private boolean handleActive(CommandSender sender, QuestService questService) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("Only players can view active quests.", NamedTextColor.RED));
            return true;
        }
        if (!(questService instanceof QuestServiceImpl questServiceImpl)) {
            sender.sendMessage(c("Quest service not ready.", NamedTextColor.RED));
            return true;
        }

        List<String> active = questServiceImpl.getActiveQuestIds(player);
        if (active.isEmpty()) {
            sender.sendMessage(c("No active quests.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(c("Active quests:", NamedTextColor.GREEN));
        for (String questId : active) {
            QuestDefinition quest = questServiceImpl.getQuestDefinition(questId);

            if (quest == null) {
                sender.sendMessage(Component.text("- " + questId, NamedTextColor.GRAY));
                continue;
            }

            QuestProgress progress = questService.getProgress(player, quest.id());
            sender.sendMessage(Component.text("- ", NamedTextColor.GOLD)
                    .append(Component.text(quest.id(), NamedTextColor.GOLD)));

            for (ObjectiveDefinition objective : quest.objectives()) {
                boolean completed = progress != null && progress.completedObjectives().contains(objective.id());

                Component status = Component.text(completed ? "✓" : "•",
                        completed ? NamedTextColor.GREEN : NamedTextColor.GRAY);

                String description = questServiceImpl.describeObjective(objective);

                Component line = status
                        .append(Component.space())
                        .append(Component.text(description, NamedTextColor.AQUA));

                if (!completed) {
                    int amount = objective.amount();
                    if (amount > 1) {
                        int current = questServiceImpl.getObjectiveProgressCount(
                                player, objective,
                                progress instanceof QuestProgressData pd ? pd : null);
                        line = line.append(Component.text(" (" + current + "/" + amount + ")", NamedTextColor.YELLOW));
                    }
                }

                sender.sendMessage(line);
            }
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, QuestService questService) {
        if (!sender.hasPermission("realite.quests.admin") && !sender.isOp()) {
            sender.sendMessage(c("No permission.", NamedTextColor.RED));
            return true;
        }
        if (!(questService instanceof QuestServiceImpl questServiceImpl)) {
            sender.sendMessage(c("Quest service not ready.", NamedTextColor.RED));
            return true;
        }
        questServiceImpl.reloadQuests();
        sender.sendMessage(c("Quest definitions reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleMenu(CommandSender sender, QuestService questService) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("Only players can open quest menu.", NamedTextColor.RED));
            return true;
        }
        if (!(questService instanceof QuestServiceImpl questServiceImpl)) {
            sender.sendMessage(c("Quest service not ready.", NamedTextColor.RED));
            return true;
        }
        new QuestsHubMenu(questServiceImpl, questServiceImpl.messages(), player,
                QuestMenuState.defaultState()).open(player);
        return true;
    }

    private boolean handleDebug(CommandSender sender, QuestService questService, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(c("Only players can debug quests.", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("realite.quests.admin") && !sender.isOp()) {
            sender.sendMessage(c("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(c("Usage: /quest debug <id>", NamedTextColor.RED));
            return true;
        }
        if (!(questService instanceof QuestServiceImpl questServiceImpl)) {
            sender.sendMessage(c("Quest service not ready.", NamedTextColor.RED));
            return true;
        }

        String questId = args[1];
        QuestDefinition quest = questServiceImpl.getQuestDefinition(questId);
        if (quest == null) {
            sender.sendMessage(Component.text("Quest not found: ", NamedTextColor.RED)
                    .append(Component.text(questId, NamedTextColor.GOLD)));
            return true;
        }

        QuestProgress progress = questService.getProgress(player, quest.id());
        sender.sendMessage(Component.text("Quest debug: ", NamedTextColor.GOLD)
                .append(Component.text(quest.id(), NamedTextColor.GOLD)));

        if (progress == null) {
            sender.sendMessage(c("No progress data.", NamedTextColor.YELLOW));
        }

        Map<String, Integer> counts = progress instanceof QuestProgressData pd ? pd.objectiveCounts() : Map.of();

        for (ObjectiveDefinition objective : quest.objectives()) {
            boolean completed = progress != null && progress.completedObjectives().contains(objective.id());

            Component status = Component.text(completed ? "COMPLETED" : "INCOMPLETE",
                    completed ? NamedTextColor.GREEN : NamedTextColor.GRAY);

            Component line = Component.text("- " + objective.id(), NamedTextColor.AQUA)
                    .append(Component.text(" [" + objective.type() + "] ", NamedTextColor.DARK_GRAY))
                    .append(status);

            if (!completed && counts.containsKey(objective.id())) {
                line = line.append(Component.text(
                        " (" + counts.get(objective.id()) + "/" + objective.amount() + ")",
                        NamedTextColor.YELLOW));
            }
            if (!completed) {
                ConditionCheckResult conditionResult = questServiceImpl
                        .getObjectiveConditionResult(player, quest, objective);
                if (!conditionResult.allowed() && conditionResult.reasonKey() != null) {
                    line = line.append(Component.text(" [" + conditionResult.reasonKey() + "]",
                            NamedTextColor.RED));
                }
            }

            sender.sendMessage(line);
        }

        return true;
    }

    private static Component c(String text, NamedTextColor color) {
        return Component.text(text, color);
    }
}
