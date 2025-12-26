package ru.realite.classes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ClassSettingsMenu implements InventoryHolder {

    public static final String TITLE = "Class Settings";

    private final Inventory inv;

    public ClassSettingsMenu() {
        this.inv = Bukkit.createInventory(this, 9, TITLE);
        build();
    }

    private void build() {
        inv.clear();

        inv.setItem(1, item(Material.DRAGON_BREATH, "§6BossBar", List.of(
                "§7Полоска сверху экрана",
                "§7Рекомендуется (дефолт)"
        )));

        inv.setItem(3, item(Material.PAPER, "§aActionBar", List.of(
                "§7Текст над хотбаром снизу",
                "§7Может перетираться другими плагинами"
        )));

        inv.setItem(5, item(Material.OAK_SIGN, "§bSidebar", List.of(
                "§7Список справа (scoreboard)",
                "§7Может конфликтовать с другими scoreboard"
        )));

        inv.setItem(7, item(Material.BARRIER, "§cOff", List.of(
                "§7Скрыть HUD класса"
        )));
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }
}
