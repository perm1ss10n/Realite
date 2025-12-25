package ru.realite.classes.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.classes.model.ItemAmount;

import java.util.List;

public final class InvUtil {
    private InvUtil() {}

    public static boolean hasItems(Player p, List<ItemAmount> req) {
        if (req == null || req.isEmpty()) return true;

        for (ItemAmount ia : req) {
            int need = ia.amount();
            if (need <= 0) continue;

            int have = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                if (it.getType() != ia.material()) continue;
                have += it.getAmount();
                if (have >= need) break;
            }
            if (have < need) return false;
        }
        return true;
    }

    public static void takeItems(Player p, List<ItemAmount> req) {
        if (req == null || req.isEmpty()) return;

        for (ItemAmount ia : req) {
            Material mat = ia.material();
            int toRemove = ia.amount();
            if (toRemove <= 0) continue;

            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < contents.length && toRemove > 0; i++) {
                ItemStack it = contents[i];
                if (it == null || it.getType() != mat) continue;

                int amt = it.getAmount();
                if (amt <= toRemove) {
                    contents[i] = null;
                    toRemove -= amt;
                } else {
                    it.setAmount(amt - toRemove);
                    toRemove = 0;
                }
            }
            p.getInventory().setContents(contents);
        }
        p.updateInventory();
    }

    public static void giveItems(Player p, List<ItemAmount> items) {
        if (items == null || items.isEmpty()) return;

        for (ItemAmount ia : items) {
            if (ia.amount() <= 0) continue;
            p.getInventory().addItem(new ItemStack(ia.material(), ia.amount()));
        }
    }
}
