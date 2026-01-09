package ru.realite.magic.integration.items;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ItemsBridge {

    boolean hasItem(Player player, String itemId, int amount);

    boolean removeItem(Player player, String itemId, int amount);

    boolean isItem(ItemStack stack, String itemId);

    Optional<String> getItemId(ItemStack stack);

    Component displayName(String itemId);

    void give(Player player, String itemId, int amount);

    OptionalInt readInt(ItemStack stack, String key);

    OptionalDouble readDouble(ItemStack stack, String key);

    Optional<String> readString(ItemStack stack, String key);
}
