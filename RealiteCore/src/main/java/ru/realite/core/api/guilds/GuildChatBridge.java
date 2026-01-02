package ru.realite.core.api.guilds;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface GuildChatBridge {

    boolean isEnabled();

    boolean isToggleCommandEnabled();

    boolean isMember(Player player);

    boolean isToggled(Player player);

    boolean toggle(Player player);

    void clearToggle(Player player);

    List<Player> getGuildRecipients(Player sender);

    List<Player> getSpyRecipients(Player sender);

    Component format(Player sender, Component message);

    boolean isSpyEnabled();

    String getSpyPermission();
}
