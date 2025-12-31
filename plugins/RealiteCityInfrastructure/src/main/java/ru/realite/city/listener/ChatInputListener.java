package ru.realite.city.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.city.CityConfig;
import ru.realite.city.service.ChatInputService;

public final class ChatInputListener implements Listener {

    private final JavaPlugin plugin;
    private final CityConfig config;
    private final ChatInputService chatInputService;

    public ChatInputListener(JavaPlugin plugin, CityConfig config, ChatInputService chatInputService) {
        this.plugin = plugin;
        this.config = config;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!chatInputService.hasPending(player)) {
            return;
        }

        if (config.chatInputHideMessage()) {
            event.setCancelled(true);
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // переносим в основной поток (Bukkit API + твои сервисы)
        Bukkit.getScheduler().runTask(plugin, () -> chatInputService.handleChat(player, message));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!chatInputService.hasPending(player)) {
            return;
        }

        String raw = event.getMessage();
        if (raw == null)
            return;

        String trimmed = raw.trim();

        // берем первую "команду" до пробела
        String cmd = trimmed;
        int space = trimmed.indexOf(' ');
        if (space >= 0) {
            cmd = trimmed.substring(0, space);
        }

        if ("/cancel".equalsIgnoreCase(cmd)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> chatInputService.handleCancel(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatInputService.clear(event.getPlayer());
    }
}
