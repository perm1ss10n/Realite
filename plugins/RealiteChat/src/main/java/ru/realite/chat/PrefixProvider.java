package ru.realite.chat;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface PrefixProvider {

    Optional<Component> getPrefix(Player player);
}
