package ru.realite.familiars.service;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class FamiliarVirtualInventoryHolder implements InventoryHolder {

    private final UUID ownerId;
    private final String typeId;
    private Inventory inventory;

    public FamiliarVirtualInventoryHolder(UUID ownerId, String typeId) {
        this.ownerId = ownerId;
        this.typeId = typeId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String typeId() {
        return typeId;
    }

    public void attachInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
