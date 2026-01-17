package ru.realite.familiars.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.model.FamiliarBehavior;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.menu.FamiliarMenuManager;
import ru.realite.familiars.service.CheckResult;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.ui.FamiliarActionBarService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FamiliarCommand implements CommandExecutor {

    private final FamiliarService service;
    private final Messages messages;
    private final FamiliarActionBarService actionBar;
    private final FamiliarMenuManager menuManager;

    public FamiliarCommand(FamiliarService service,
                           Messages messages,
                           FamiliarActionBarService actionBar,
                           FamiliarMenuManager menuManager) {
        this.service = service;
        this.messages = messages;
        this.actionBar = actionBar;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (service == null) {
            sender.sendMessage(messages.get("familiar.no-service"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(messages.get("familiar.usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "summon" -> handleSummon(player, resolveType(player, args, 1));
            case "dismiss" -> handleDismiss(player, resolveType(player, args, 1));
            case "follow" -> handleBehavior(player, resolveType(player, args, 1), FamiliarBehavior.FOLLOW);
            case "stay" -> handleBehavior(player, resolveType(player, args, 1), FamiliarBehavior.STAY);
            case "ui", "menu" -> handleMenu(player);
            default -> sender.sendMessage(messages.get("familiar.usage"));
        }

        return true;
    }

    private String resolveType(Player player, String[] args, int index) {
        if (args.length > index) {
            return args[index];
        }
        List<FamiliarInstance> familiars = service.getFamiliars(player.getUniqueId());
        if (familiars.isEmpty()) {
            player.sendMessage(messages.get("familiar.no-familiars"));
            return null;
        }
        if (familiars.size() == 1) {
            return familiars.get(0).typeId();
        }
        String types = familiars.stream()
                .map(FamiliarInstance::typeId)
                .sorted()
                .collect(Collectors.joining(", "));
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("types", types);
        player.sendMessage(messages.get("familiar.specify-type", placeholders));
        return null;
    }

    private void handleSummon(Player player, String typeId) {
        if (typeId == null) {
            return;
        }
        CheckResult result = service.summon(player, typeId);
        if (result.allowed()) {
            sendSimple(player, "familiar.summon.success", typeId);
        } else {
            sendFailure(player, "familiar.summon.failure", result);
        }
    }

    private void handleDismiss(Player player, String typeId) {
        if (typeId == null) {
            return;
        }
        CheckResult result = service.dismiss(player, typeId);
        if (result.allowed()) {
            sendSimple(player, "familiar.dismiss.success", typeId);
        } else {
            sendFailure(player, "familiar.dismiss.failure", result);
        }
    }

    private void handleBehavior(Player player, String typeId, FamiliarBehavior behavior) {
        if (typeId == null) {
            return;
        }
        CheckResult result = service.setBehavior(player, typeId, behavior);
        if (result.allowed()) {
            String key = behavior == FamiliarBehavior.FOLLOW
                    ? "familiar.behavior.follow"
                    : "familiar.behavior.stay";
            sendSimple(player, key, typeId);
        } else {
            sendFailure(player, "familiar.behavior.failure", result);
        }
    }

    private void handleMenu(Player player) {
        if (menuManager == null) {
            player.sendMessage(messages.get("familiar.menu.unavailable"));
            return;
        }
        menuManager.openMain(player);
    }

    private void sendSimple(Player player, String key, String typeId) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", typeId);
        player.sendMessage(messages.get(key, placeholders));
    }

    private void sendFailure(Player player, String key, CheckResult result) {
        player.sendMessage(messages.get(key));
        if (actionBar != null) {
            actionBar.sendForReasons(player, result.reasons());
        }
        if (!result.reasons().isEmpty()) {
            player.sendMessage(messages.get("familiar.reasons"));
            for (String reason : result.reasons()) {
                player.sendMessage(Component.text(" - " + reason));
            }
        }
    }
}
