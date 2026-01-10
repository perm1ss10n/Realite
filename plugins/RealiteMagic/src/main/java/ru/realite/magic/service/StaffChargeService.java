package ru.realite.magic.service;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.MagicItemTags;

public final class StaffChargeService {

    private final ItemsBridge itemsBridge;

    public StaffChargeService(ItemsBridge itemsBridge) {
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
    }

    public Optional<StaffItem> findStaff(Player player, boolean allowOffhand) {
        if (player == null) {
            return Optional.empty();
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isStaff(main)) {
            return Optional.of(new StaffItem(main, false));
        }
        if (allowOffhand) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (isStaff(offhand)) {
                return Optional.of(new StaffItem(offhand, true));
            }
        }
        return Optional.empty();
    }

    public boolean isStaff(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        return itemsBridge.readInt(stack, MagicItemTags.STAFF).orElse(0) > 0;
    }

    public StaffCharges readCharges(ItemStack stack) {
        int current = readInt(stack, MagicItemTags.STAFF_CHARGES).orElse(0);
        int max = readInt(stack, MagicItemTags.STAFF_MAX_CHARGES).orElse(0);
        if (max > 0 && current > max) {
            current = max;
        }
        return new StaffCharges(current, max);
    }

    public boolean writeCharges(Player player, StaffItem staffItem, int newCharges) {
        if (player == null || staffItem == null) {
            return false;
        }
        ItemStack stack = staffItem.stack();
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        StaffCharges charges = readCharges(stack);
        int clamped = newCharges;
        if (charges.max() > 0) {
            clamped = Math.min(charges.max(), Math.max(0, newCharges));
        } else {
            clamped = Math.max(0, newCharges);
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(MagicItemTags.key(MagicItemTags.STAFF_CHARGES), PersistentDataType.INTEGER, clamped);
        stack.setItemMeta(meta);
        if (staffItem.offhand()) {
            player.getInventory().setItemInOffHand(stack);
        } else {
            player.getInventory().setItemInMainHand(stack);
        }
        return true;
    }

    private OptionalInt readInt(ItemStack stack, String key) {
        if (stack == null || stack.getType() == Material.AIR) {
            return OptionalInt.empty();
        }
        return itemsBridge.readInt(stack, key);
    }

    public record StaffItem(ItemStack stack, boolean offhand) {
    }

    public record StaffCharges(int current, int max) {
    }
}
