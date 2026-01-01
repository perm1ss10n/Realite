package ru.realite.chat;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public final class RealiteChatCommand implements TabExecutor {

    private final RealiteChatPlugin plugin;

    public RealiteChatCommand(RealiteChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!plugin.hasReloadPermission(sender)) {
                sender.sendMessage(plugin.getMessages().get("chat.reload.no-permission"));
                return true;
            }
            plugin.reloadAll();
            sender.sendMessage(plugin.getMessages().get("chat.reload.success"));
            return true;
        }

        sender.sendMessage(plugin.getMessages().get("chat.reload.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            if ("reload".startsWith(a))
                return List.of("reload");
        }
        return List.of();
    }
}
