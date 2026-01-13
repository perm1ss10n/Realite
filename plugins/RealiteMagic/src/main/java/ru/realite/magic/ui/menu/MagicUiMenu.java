package ru.realite.magic.ui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface MagicUiMenu {
    void handleClick(Player player, InventoryClickEvent event);
}
