package ru.realite.familiars.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.service.CheckResult;
import ru.realite.familiars.service.FamiliarService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FamiliarsCommand implements CommandExecutor {

    private final FamiliarService service;
    private final Messages messages;

    public FamiliarsCommand(FamiliarService service, Messages messages) {
        this.service = service;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !"debug".equalsIgnoreCase(args[0])) {
            sender.sendMessage(messages.get("debug.usage"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        if (service == null) {
            sender.sendMessage(messages.get("debug.no-service"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("debug.usage"));
            return true;
        }
        String typeId = args[1];
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("type", typeId);
        sender.sendMessage(messages.get("debug.header", placeholders));

        CheckResult tameResult = service.canTame(player, typeId);
        CheckResult summonResult = service.canSummon(player, typeId);

        sendResult(sender, "debug.can-tame", tameResult);
        sendResult(sender, "debug.can-summon", summonResult);

        return true;
    }

    private void sendResult(CommandSender sender, String key, CheckResult result) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("result", result.allowed() ? "<green>YES</green>" : "<red>NO</red>");
        sender.sendMessage(messages.get(key, placeholders));
        if (!result.reasons().isEmpty()) {
            sendList(sender, messages.get("debug.reasons"), result.reasons());
        }
        if (!result.notes().isEmpty()) {
            sendList(sender, messages.get("debug.notes"), result.notes());
        }
    }

    private void sendList(CommandSender sender, Component header, List<String> lines) {
        sender.sendMessage(header);
        for (String line : lines) {
            sender.sendMessage(Component.text(" - " + line));
        }
    }
}
