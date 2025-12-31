package ru.realite.city.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!chatInputService.hasPending(player)) {
            return;
        }
        if (config.chatInputHideMessage()) {
            event.setCancelled(true);
        }
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> chatInputService.handleChat(player, message));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!chatInputService.hasPending(player)) {
            return;
        }
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        if ("/cancel".equalsIgnoreCase(message.trim())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> chatInputService.handleCancel(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatInputService.clear(event.getPlayer());
    }
}
