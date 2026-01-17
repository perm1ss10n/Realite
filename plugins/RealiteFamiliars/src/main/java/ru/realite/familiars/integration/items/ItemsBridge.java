package ru.realite.familiars.integration.items;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public interface ItemsBridge {

    boolean isAvailable();

    Optional<String> getItemId(ItemStack stack);

    boolean isItem(ItemStack stack, String itemId);

    Optional<Integer> readInt(ItemStack stack, String key);

    Optional<String> readString(ItemStack stack, String key);
}
