package ru.realite.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealiteChatPlugin extends JavaPlugin implements Listener {
    private PrefixProvider prefixProvider = player -> Optional.empty();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefixProvider = resolvePrefixProvider();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();
        String classTag = getConfig().getString("chat.class-tag", "[Бродяга-I]");
        Component classTagComponent = Component.text(classTag);
        Component luckPermsPrefix = prefixProvider.getPrefix(event.getPlayer()).orElse(Component.empty());
        Component tags = luckPermsPrefix.append(classTagComponent);
        Component prefix = tags.append(Component.text(" "))
                .append(Component.text(playerName))
                .append(Component.text(": "));

        event.renderer((source, sourceDisplayName, message, viewer) -> prefix.append(message));
    }

    private PrefixProvider resolvePrefixProvider() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager()
                .getRegistration(LuckPerms.class);
        if (provider == null) {
            return player -> Optional.empty();
        }
        return new LuckPermsPrefixProvider(provider.getProvider());
    }
}
