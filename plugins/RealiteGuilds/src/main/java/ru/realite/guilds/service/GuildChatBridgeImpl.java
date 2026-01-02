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
    public boolean isToggleCommandEnabled() {
        return chatService.isToggleCommandEnabled();
    }

    @Override
    public boolean isMember(Player player) {
        return chatService.isMember(player);
    }

    @Override
    public boolean isToggled(Player player) {
        return chatService.isToggled(player);
    }

    @Override
    public boolean toggle(Player player) {
        return chatService.toggleAndNotify(player);
    }

    @Override
    public void clearToggle(Player player) {
        chatService.clearToggle(player);
    }

    @Override
    public List<Player> getGuildRecipients(Player sender) {
        return chatService.getGuildRecipients(sender);
    }

    @Override
    public List<Player> getSpyRecipients(Player sender) {
        return chatService.getSpyRecipients(sender);
    }

    @Override
    public Component format(Player sender, Component message) {
        return chatService.format(sender, message);
    }

    @Override
    public boolean isSpyEnabled() {
        return chatService.isSpyEnabled();
    }

    @Override
    public String getSpyPermission() {
        return chatService.getSpyPermission();
    }
}
