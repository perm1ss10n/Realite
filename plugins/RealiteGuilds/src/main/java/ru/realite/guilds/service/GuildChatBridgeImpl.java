package ru.realite.guilds.service;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ru.realite.core.api.guilds.GuildChatBridge;

public final class GuildChatBridgeImpl implements GuildChatBridge {

    private final GuildChatService chatService;

    public GuildChatBridgeImpl(GuildChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean isEnabled() {
        return chatService.isGuildChatEnabled();
    }

    @Override
    public boolean isMember(Player player) {
        return chatService.isMember(player);
    }

    @Override
    public List<Player> getGuildRecipients(Player sender) {
        return chatService.getGuildRecipients(sender);
    }

    @Override
    public List<Player> getSpyRecipients(Player sender) {
        return chatService.getSpyRecipients(sender);
    }

    /**
     * Форматирование гильд-чата теперь полностью в RealiteChat.
     * Здесь просто pass-through.
     */
    @Override
    public Component format(Player sender, Component message) {
        return message;
    }

    @Override
    public boolean isSpyEnabled() {
        return chatService.isSpyEnabled();
    }

    @Override
    public String getSpyPermission() {
        return chatService.getSpyPermission();
    }

    @Override
    public String getGuildRank(Player player) {
        return chatService.getGuildRankDisplay(player);
    }
}
