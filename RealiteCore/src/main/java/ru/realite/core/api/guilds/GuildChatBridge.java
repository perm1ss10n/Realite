package ru.realite.core.api.guilds;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface GuildChatBridge {

    boolean isEnabled();

    boolean isMember(Player player);

    List<Player> getGuildRecipients(Player sender);

    List<Player> getSpyRecipients(Player sender);

    Component format(Player sender, Component message);

    boolean isSpyEnabled();

    String getSpyPermission();
}
