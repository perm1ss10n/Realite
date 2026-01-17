package ru.realite.familiars.integration.items;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.realite.items.service.ItemService;

public final class CoreItemsBridge implements ItemsBridge {

    private final ItemService itemService;

    public CoreItemsBridge(ItemService itemService) {
        this.itemService = Objects.requireNonNull(itemService, "itemService");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Optional<String> getItemId(ItemStack stack) {
        return itemService.getItemId(stack);
    }

    @Override
    public boolean isItem(ItemStack stack, String itemId) {
        return itemService.isItem(stack, itemId);
    }

    @Override
    public Optional<Integer> readInt(ItemStack stack, String key) {
        PersistentDataContainer container = container(stack);
        if (container == null) {
            return Optional.empty();
        }
        NamespacedKey namespacedKey = new NamespacedKey("realite", key);
        return Optional.ofNullable(container.get(namespacedKey, PersistentDataType.INTEGER));
    }

    @Override
    public Optional<String> readString(ItemStack stack, String key) {
        PersistentDataContainer container = container(stack);
        if (container == null) {
            return Optional.empty();
        }
        NamespacedKey namespacedKey = new NamespacedKey("realite", key);
        return Optional.ofNullable(container.get(namespacedKey, PersistentDataType.STRING));
    }

    private PersistentDataContainer container(ItemStack stack) {
        if (stack == null || stack.getItemMeta() == null) {
            return null;
        }
        return stack.getItemMeta().getPersistentDataContainer();
    }
}
