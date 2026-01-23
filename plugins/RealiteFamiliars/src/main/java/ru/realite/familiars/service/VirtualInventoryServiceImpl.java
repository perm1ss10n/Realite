package ru.realite.familiars.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.realite.familiars.integration.items.ItemsBridge;
import ru.realite.familiars.model.FamiliarInstance;

public final class VirtualInventoryServiceImpl implements VirtualInventoryService {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final int SLOT_COUNT = 9;

    private final FamiliarService service;
    private final ItemsBridge itemsBridge;

    public VirtualInventoryServiceImpl(FamiliarService service, ItemsBridge itemsBridge) {
        this.service = service;
        this.itemsBridge = itemsBridge;
    }

    @Override
    public int slotCount() {
        return SLOT_COUNT;
    }

    @Override
    public boolean canStore(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return true;
        }
        if (itemsBridge != null && itemsBridge.isAvailable()) {
            return itemsBridge.getItemId(stack).isEmpty();
        }
        return true;
    }

    @Override
    public List<ItemStack> loadInventory(UUID owner, String typeId) {
        if (service == null || owner == null || typeId == null) {
            return List.of();
        }
        FamiliarInstance instance = findInstance(owner, typeId);
        if (instance == null) {
            return List.of();
        }
        return normalize(instance.inventory());
    }

    @Override
    public void saveInventory(UUID owner, String typeId, List<ItemStack> items) {
        if (service == null || owner == null || typeId == null) {
            return;
        }
        List<ItemStack> normalized = normalize(items);
        service.updateInventory(owner, typeId, normalized);
    }

    @Override
    public List<String> describe(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            String name = describeItem(stack);
            if (!name.isBlank()) {
                result.add(name);
            }
        }
        return result;
    }

    @Override
    public boolean open(Player player, FamiliarInstance instance, String displayName) {
        if (player == null || instance == null) {
            return false;
        }
        String title = "Familiar Inventory: " + (displayName == null || displayName.isBlank()
                ? instance.typeId()
                : displayName);
        FamiliarVirtualInventoryHolder holder = new FamiliarVirtualInventoryHolder(instance.owner(), instance.typeId());
        Inventory inventory = Bukkit.createInventory(holder, SLOT_COUNT, Component.text(title));
        holder.attachInventory(inventory);
        List<ItemStack> contents = loadInventory(instance.owner(), instance.typeId());
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = i < contents.size() ? contents.get(i) : null;
            inventory.setItem(i, stack);
        }
        player.openInventory(inventory);
        return true;
    }

    private List<ItemStack> normalize(List<ItemStack> items) {
        List<ItemStack> safe = (items == null) ? List.of() : items;

        List<ItemStack> normalized = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = (i < safe.size()) ? safe.get(i) : null;

            if (stack == null || stack.getType() == Material.AIR) {
                normalized.add(null);
            } else {
                normalized.add(stack.clone());
            }
        }
        return List.copyOf(normalized);
    }

    private FamiliarInstance findInstance(UUID owner, String typeId) {
        for (FamiliarInstance instance : service.getFamiliars(owner)) {
            if (instance.typeId().equalsIgnoreCase(typeId)) {
                return instance;
            }
        }
        return null;
    }

    private String describeItem(ItemStack stack) {
        String name = resolveName(stack);
        int amount = Math.max(1, stack.getAmount());
        if (amount == 1) {
            return name;
        }
        return name + " x" + amount;
    }

    private String resolveName(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            String plain = PLAIN.serialize(Objects.requireNonNull(stack.getItemMeta().displayName()));
            if (!plain.isBlank()) {
                return plain;
            }
        }
        return stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
