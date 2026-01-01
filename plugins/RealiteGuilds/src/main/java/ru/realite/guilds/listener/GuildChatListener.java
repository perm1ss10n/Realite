package ru.realite.guilds.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        // TODO: После интеграции с RealiteChat этот перехват надо удалить:
        // гильдийский чат будет отправляться из RealiteChat через bridge.

        // 1) Guild chat toggle: полностью перехватываем и уходим в гильдейский канал
        if (chatService.isGuildChatEnabled() && chatService.isToggled(event.getPlayer())) {
            event.setCancelled(true);
            chatService.sendGuildChatAsync(event.getPlayer(), event.message());
            return;
        }

        // 2) Публичные префиксы/теги НЕ рисуем здесь через renderer.
        // Этим занимается RealiteChat через {guild} + GuildTagProvider.
        // Если RealiteChat нет — просто ничего не делаем (не ломаем чужие чат-плагины).
        if (!realiteChatAvailable) {
            return;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatService.clearToggle(event.getPlayer());
    }
}
