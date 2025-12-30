package ru.realite.guilds.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildChatService;

public final class GuildChatCommand implements CommandExecutor {

    private final GuildChatService chatService;
    private final GuildMessages messages;

    public GuildChatCommand(GuildChatService chatService, GuildMessages messages) {
        this.chatService = chatService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("error.player_only"));
            return true;
        }
        if (args.length == 0) {
            messages.send(player, "usage.chat");
            return true;
        }
        String message = String.join(" ", args);
        chatService.sendGuildChat(player, Component.text(message));
        return true;
    }
}
