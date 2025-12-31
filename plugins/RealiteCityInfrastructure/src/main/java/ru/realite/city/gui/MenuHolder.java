package ru.realite.city.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuHolder implements InventoryHolder {

    private final MenuType menuType;

    public MenuHolder(MenuType menuType) {
        this.menuType = menuType;
    }

    public MenuType menuType() {
        return menuType;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
