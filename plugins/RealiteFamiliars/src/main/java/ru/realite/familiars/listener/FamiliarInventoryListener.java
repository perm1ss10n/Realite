package ru.realite.familiars.listener;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.realite.familiars.service.FamiliarVirtualInventoryHolder;
import ru.realite.familiars.service.VirtualInventoryService;

public final class FamiliarInventoryListener implements Listener {

    private final VirtualInventoryService inventoryService;

    public FamiliarInventoryListener(VirtualInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof FamiliarVirtualInventoryHolder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (inventoryService == null) {
            event.setCancelled(true);
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (event.isShiftClick() && clicked.equals(player.getInventory())) {
            ItemStack moving = event.getCurrentItem();
            if (!inventoryService.canStore(moving)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!clicked.equals(top)) {
            return;
        }
        if (event.getAction() == InventoryAction.NOTHING) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (event.getHotbarButton() >= 0 && event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (!inventoryService.canStore(hotbar)) {
                event.setCancelled(true);
            }
            return;
        }
        if (!inventoryService.canStore(cursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof FamiliarVirtualInventoryHolder)) {
            return;
        }
        if (inventoryService == null) {
            event.setCancelled(true);
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top == null || top.getType() != InventoryType.CHEST) {
            return;
        }
        for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
            if (entry.getKey() < top.getSize() && !inventoryService.canStore(entry.getValue())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof FamiliarVirtualInventoryHolder familiarHolder)) {
            return;
        }
        if (inventoryService == null) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        inventoryService.saveInventory(familiarHolder.ownerId(), familiarHolder.typeId(),
                Arrays.asList(top.getContents()));
    }
}
