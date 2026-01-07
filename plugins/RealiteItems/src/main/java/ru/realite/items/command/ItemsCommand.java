package ru.realite.items.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import ru.realite.items.i18n.ItemMessages;
import ru.realite.items.service.ItemRegistry;
import ru.realite.items.service.ItemService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ItemsCommand implements CommandExecutor, TabCompleter {

    private final ItemService itemService;
    private final ItemMessages messages;
    private final ItemRegistry registry;

    public ItemsCommand(ItemService itemService, ItemMessages messages, ItemRegistry registry) {
        this.itemService = itemService;
        this.messages = messages;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("realite.items.admin")) {
            messages.send(sender, "items.command.no-permission", "");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            messages.send(sender, "items.command.usage", "");
            return true;
        }

        if (args.length < 3) {
            messages.send(sender, "items.command.usage", "");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            messages.send(sender, "items.command.player-not-found", "");
            return true;
        }

        String itemId = args[2];
        if (registry.get(itemId).isEmpty()) {
            messages.send(sender, "items.command.item-not-found", "", Map.of("itemId", itemId));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                messages.send(sender, "items.command.invalid-amount", "");
                return true;
            }
        }
        if (amount <= 0) {
            messages.send(sender, "items.command.invalid-amount", "");
            return true;
        }

        ItemStack stack = itemService.create(itemId, amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), item);
            }
        }

        Map<String, String> vars = Map.of(
                "player", target.getName(),
                "itemId", itemId,
                "amount", String.valueOf(amount)
        );
        messages.send(sender, "items.command.give-sender", "", vars);
        if (!sender.getName().equalsIgnoreCase(target.getName())) {
            messages.send(target, "items.command.give-receiver", "", Map.of(
                    "itemId", itemId,
                    "amount", String.valueOf(amount)
            ));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("realite.items.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return List.of("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return new ArrayList<>(registry.items().keySet());
        }
        return Collections.emptyList();
    }
}
