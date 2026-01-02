package ru.realite.chat;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.core.api.guilds.GuildChatBridge;

public final class GuildChatMessageCommand implements CommandExecutor {

    private final RealiteChatPlugin plugin;

    public GuildChatMessageCommand(RealiteChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Command available for players only."));
            return true;
        }

        GuildChatBridge bridge = plugin.getGuildChatBridge();
        if (bridge == null) {
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /gc <сообщение>"));
            return true;
        }

        String text = String.join(" ", args);
        plugin.sendGuildChat(player, Component.text(text));
        return true;
    }
}
