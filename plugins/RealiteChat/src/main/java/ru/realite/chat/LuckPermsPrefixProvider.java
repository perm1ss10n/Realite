package ru.realite.chat;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

public final class LuckPermsPrefixProvider implements PrefixProvider {
    private final LuckPerms luckPerms;

    public LuckPermsPrefixProvider(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    @Override
    public Optional<Component> getPrefix(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return Optional.empty();
        }
        String rawPrefix = user.getCachedData().getMetaData().getPrefix();
        if (rawPrefix == null) {
            return Optional.empty();
        }
        String trimmedPrefix = rawPrefix.trim();
        if (trimmedPrefix.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Component.text("[" + trimmedPrefix + "]"));
    }
}
