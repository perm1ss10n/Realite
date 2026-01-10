package ru.realite.magic.integration.items;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.items.service.ItemService;

public final class CoreItemsBridge implements ItemsBridge {

    private final Supplier<ItemService> itemServiceSupplier;

    public CoreItemsBridge() {
        this(CoreItemsBridge::resolveItemService);
    }

    public CoreItemsBridge(Supplier<ItemService> itemServiceSupplier) {
        this.itemServiceSupplier = Objects.requireNonNull(itemServiceSupplier, "itemServiceSupplier");
    }

    @Override
    public boolean hasItem(Player player, String itemId, int amount) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return false;
        }
        int remaining = amount;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (!itemService.isItem(stack, itemId)) {
                continue;
            }
            remaining -= stack.getAmount();
            if (remaining <= 0) {
                return true;
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (itemService.isItem(offHand, itemId)) {
            remaining -= offHand.getAmount();
        }
        return remaining <= 0;
    }

    @Override
    public boolean removeItem(Player player, String itemId, int amount) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return false;
        }
        if (!hasItem(player, itemId, amount)) {
            return false;
        }
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!itemService.isItem(stack, itemId)) {
                continue;
            }
            int stackAmount = stack.getAmount();
            if (stackAmount > remaining) {
                stack.setAmount(stackAmount - remaining);
                contents[i] = stack;
                remaining = 0;
                break;
            }
            contents[i] = null;
            remaining -= stackAmount;
            if (remaining <= 0) {
                break;
            }
        }
        player.getInventory().setStorageContents(contents);
        if (remaining > 0) {
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (itemService.isItem(offHand, itemId)) {
                int stackAmount = offHand.getAmount();
                if (stackAmount > remaining) {
                    offHand.setAmount(stackAmount - remaining);
                    player.getInventory().setItemInOffHand(offHand);
                    remaining = 0;
                } else {
                    player.getInventory().setItemInOffHand(null);
                    remaining -= stackAmount;
                }
            }
        }
        return remaining <= 0;
    }

    @Override
    public boolean isItem(ItemStack stack, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return false;
        }
        return itemService.isItem(stack, itemId);
    }

    @Override
    public Optional<String> getItemId(ItemStack stack) {
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return Optional.empty();
        }
        return itemService.getItemId(stack);
    }

    @Override
    public Component displayName(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Component.empty();
        }
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return Component.text(itemId);
        }
        try {
            ItemStack stack = itemService.create(itemId, 1);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component displayName = meta.displayName();
                if (displayName != null && !displayName.equals(Component.empty())) {
                    return displayName;
                }
            }
        } catch (IllegalArgumentException ex) {
            return Component.text(itemId);
        }
        return Component.text(itemId);
    }

    @Override
    public void give(Player player, String itemId, int amount) {
        if (player == null || itemId == null || itemId.isBlank() || amount <= 0) {
            return;
        }
        ItemService itemService = itemServiceSupplier.get();
        if (itemService == null) {
            return;
        }
        int remaining = amount;
        ItemStack sample = itemService.create(itemId, 1);
        int maxStack = Math.max(1, sample.getMaxStackSize());
        while (remaining > 0) {
            int toGive = Math.min(remaining, maxStack);
            ItemStack stack = itemService.create(itemId, toGive);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                for (ItemStack remain : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remain);
                }
            }
            remaining -= toGive;
        }
    }

    @Override
    public OptionalInt readInt(ItemStack stack, String key) {
        Integer value = readTag(stack, key, PersistentDataType.INTEGER);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    @Override
    public OptionalDouble readDouble(ItemStack stack, String key) {
        Double value = readTag(stack, key, PersistentDataType.DOUBLE);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @Override
    public Optional<String> readString(ItemStack stack, String key) {
        return Optional.ofNullable(readTag(stack, key, PersistentDataType.STRING));
    }

    private <T> T readTag(ItemStack stack, String key, PersistentDataType<?, T> type) {
        if (stack == null || key == null || key.isBlank()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(MagicItemTags.key(key), type);
    }

    private static ItemService resolveItemService() {
        RegisteredServiceProvider<ItemService> provider =
                Bukkit.getServicesManager().getRegistration(ItemService.class);
        return provider != null ? provider.getProvider() : null;
    }
}
