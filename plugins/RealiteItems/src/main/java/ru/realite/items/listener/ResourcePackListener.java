package ru.realite.items.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import ru.realite.items.RealiteItemsPlugin;
import ru.realite.items.i18n.ItemMessages;

public final class ResourcePackListener implements Listener {

    private final RealiteItemsPlugin plugin;
    private final ItemMessages messages;

    public ResourcePackListener(RealiteItemsPlugin plugin, ItemMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("resourcePack.enabled", false)) {
            return;
        }
        String url = plugin.getConfig().getString("resourcePack.url", "");
        if (url == null || url.isBlank()) {
            return;
        }
        String sha1 = plugin.getConfig().getString("resourcePack.sha1", "");
        boolean required = plugin.getConfig().getBoolean("resourcePack.required", false);
        String promptKey = plugin.getConfig().getString("resourcePack.promptMessageKey", "");

        Component prompt = null;
        if (promptKey != null && !promptKey.isBlank()) {
            Component message = messages.get(promptKey, "");
            if (!message.equals(Component.empty())) {
                prompt = message;
            }
        }

        if (sha1 != null && sha1.isBlank()) {
            sha1 = null;
        }

        event.getPlayer().setResourcePack(url, sha1, required, prompt);
    }
}
