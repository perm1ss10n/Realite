package ru.realite.magic.integration.items;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class NoopItemsBridge implements ItemsBridge {

    @Override
    public boolean hasItem(Player player, String itemId, int amount) {
        return false;
    }

    @Override
    public boolean removeItem(Player player, String itemId, int amount) {
        return false;
    }

    @Override
    public boolean isItem(ItemStack stack, String itemId) {
        return false;
    }

    @Override
    public Component displayName(String itemId) {
        return itemId == null ? Component.empty() : Component.text(itemId);
    }

    @Override
    public void give(Player player, String itemId, int amount) {
        // noop
    }
}
