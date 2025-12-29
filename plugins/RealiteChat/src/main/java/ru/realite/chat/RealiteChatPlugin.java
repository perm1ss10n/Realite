package ru.realite.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealiteChatPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();
        Component prefix = Component.text("[Бродяга-I] ")
                .append(Component.text(playerName))
                .append(Component.text(": "));

        event.renderer((source, sourceDisplayName, message, viewer) -> prefix.append(message));
    }
}
