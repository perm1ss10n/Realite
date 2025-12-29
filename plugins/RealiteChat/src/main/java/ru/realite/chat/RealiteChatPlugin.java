package ru.realite.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassTag;
import ru.realite.core.api.classes.ClassTagProvider;

public final class RealiteChatPlugin extends JavaPlugin implements Listener {
    private PrefixProvider prefixProvider = player -> Optional.empty();
    private ClassTagProvider classTagProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        prefixProvider = resolvePrefixProvider();
        classTagProvider = resolveClassTagProvider();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();
        Component classTagComponent = buildClassTagComponent(event.getPlayer());
        Component luckPermsPrefix = prefixProvider.getPrefix(event.getPlayer()).orElse(Component.empty());
        Component guildTag = Component.empty();
        Component tags = luckPermsPrefix.append(classTagComponent).append(guildTag);
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

    private ClassTagProvider resolveClassTagProvider() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        CoreApi core = provider.getProvider();
        return core.services().get(ClassTagProvider.class);
    }

    private Component buildClassTagComponent(Player player) {
        ClassTagProvider provider = classTagProvider != null ? classTagProvider : resolveClassTagProvider();
        if (provider != null) {
            classTagProvider = provider;
            ClassTag tag = provider.getTag(player);
            String romanStage = RomanNumerals.toRoman(tag.evolutionStage());
            Component hover = Component.text("Класс: ")
                    .append(Component.text(tag.displayName()))
                    .append(Component.newline())
                    .append(Component.text("Этап: " + romanStage));
            return Component.text("[" + tag.displayName() + "-" + romanStage + "]")
                    .hoverEvent(HoverEvent.showText(hover));
        }
        String classTag = getConfig().getString("chat.class-tag", "[Бродяга-I]");
        return Component.text(classTag);
    }
}
