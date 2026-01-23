package ru.realite.core.api.items;

import org.bukkit.inventory.ItemStack;

public interface ItemService {
    ItemStack create(String itemId, int amount);
}
