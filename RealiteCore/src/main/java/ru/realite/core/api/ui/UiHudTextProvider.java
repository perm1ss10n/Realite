package ru.realite.core.api.ui;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Optional provider for custom HUD text rendering.
 */
public interface UiHudTextProvider {

    Optional<Component> text(Player player, UiSlot slot);
}
