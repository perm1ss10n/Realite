package ru.realite.core.api.guilds;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface GuildChatBridge {

    boolean isEnabled();

    boolean isMember(Player player);

    List<Player> getGuildRecipients(Player sender);

    List<Player> getSpyRecipients(Player sender);

    // Legacy: больше не используем в RealiteChat, но оставляем для совместимости
    Component format(Player sender, Component message);

    // Legacy: больше не используем в RealiteChat, но оставляем для совместимости
    boolean isSpyEnabled();

    // Legacy: больше не используем в RealiteChat, но оставляем для совместимости
    String getSpyPermission();

    /**
     * Display rank of player inside their guild (localized / human readable).
     * Default empty for backward compatibility.
     */
    default String getGuildRank(Player player) {
        return "";
    }
}
