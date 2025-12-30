package ru.realite.guilds.service;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ru.realite.core.api.guilds.GuildTagProvider;

public final class GuildChatTagProvider implements GuildTagProvider {

    private final GuildChatService chatService;

    public GuildChatTagProvider(GuildChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public Optional<Component> getTag(Player player) {
        Component prefix = chatService.buildPublicPrefix(player, false);
        if (prefix.equals(Component.empty())) {
            return Optional.empty();
        }
        return Optional.of(prefix);
    }

    @Override
    public Optional<Component> getHover(Player player) {
        Component hover = chatService.buildHover(player);
        if (hover.equals(Component.empty())) {
            return Optional.empty();
        }
        return Optional.of(hover);
    }
}
