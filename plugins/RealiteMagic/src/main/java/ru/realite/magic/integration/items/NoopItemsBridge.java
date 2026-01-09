package ru.realite.magic.integration.items;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
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
    public Optional<String> getItemId(ItemStack stack) {
        return Optional.empty();
    }

    @Override
    public Component displayName(String itemId) {
        return itemId == null ? Component.empty() : Component.text(itemId);
    }

    @Override
    public void give(Player player, String itemId, int amount) {
        // noop
    }

    @Override
    public OptionalInt readInt(ItemStack stack, String key) {
        return OptionalInt.empty();
    }

    @Override
    public OptionalDouble readDouble(ItemStack stack, String key) {
        return OptionalDouble.empty();
    }

    @Override
    public Optional<String> readString(ItemStack stack, String key) {
        return Optional.empty();
    }
}
