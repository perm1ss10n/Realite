package ru.realite.guilds.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.guilds.service.GuildChatService;

public final class GuildChatListener implements Listener {

    private final GuildChatService chatService;
    private final boolean realiteChatAvailable;

    public GuildChatListener(GuildChatService chatService, boolean realiteChatAvailable) {
        this.chatService = chatService;
        this.realiteChatAvailable = realiteChatAvailable;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (chatService.isGuildChatEnabled() && chatService.isToggled(event.getPlayer())) {
            event.setCancelled(true);
            chatService.sendGuildChatAsync(event.getPlayer(), event.message());
            return;
        }
        if (realiteChatAvailable || !chatService.isPrefixEnabled()) {
            return;
        }
        Component prefix = chatService.buildPublicPrefix(event.getPlayer());
        if (prefix.equals(Component.empty())) {
            return;
        }
        event.renderer((source, sourceDisplayName, message, viewer) ->
                prefix.append(sourceDisplayName).append(Component.text(": ")).append(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatService.clearToggle(event.getPlayer());
    }
}
