package ru.realite.familiars.service;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.familiars.model.FamiliarInstance;

public interface VirtualInventoryService {

    int slotCount();

    boolean canStore(ItemStack stack);

    List<ItemStack> loadInventory(UUID owner, String typeId);

    void saveInventory(UUID owner, String typeId, List<ItemStack> items);

    List<String> describe(List<ItemStack> items);

    boolean open(Player player, FamiliarInstance instance, String displayName);
}
