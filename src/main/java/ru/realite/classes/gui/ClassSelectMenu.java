package ru.realite.classes.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.util.Components;

import java.util.ArrayList;
import java.util.List;

public class ClassSelectMenu implements InventoryHolder {

    public static final int SIZE = 27;

    private final ClassConfigRepository classConfig;
    private final Inventory inventory;
    private final NamespacedKey classIdKey;

    public ClassSelectMenu(JavaPlugin plugin, ClassConfigRepository classConfig) {
        this.classConfig = classConfig;
        this.classIdKey = new NamespacedKey(plugin, "class_id");

        this.inventory = Bukkit.createInventory(
                this,
                SIZE,
                Component.text("Выбор класса"));

        fill();
    }

    private void fill() {
        inventory.clear();

        // стабильный порядок по enum
        var defs = new java.util.ArrayList<ClassConfigRepository.ClassDef>();
        for (ru.realite.classes.model.ClassId id : ru.realite.classes.model.ClassId.values()) {
            var def = classConfig.get(id);
            if (def != null)
                defs.add(def);
        }

        int index = 0;
        int total = defs.size();
        int rows = SIZE / 9;

        for (int row = 0; row < rows && index < total; row++) {
            int rowStart = row * 9;

            int remaining = total - index;
            int countInRow = Math.min(9, remaining);

            // центрируем содержимое строки
            int startSlot = rowStart + (9 - countInRow) / 2;

            for (int i = 0; i < countInRow; i++) {
                var def = defs.get(index++);

                ItemStack item = new ItemStack(def.icon != null ? def.icon : Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(ru.realite.classes.util.Components.c("&6" + def.name));

                    List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                    if (def.lore != null) {
                        for (String line : def.lore) {
                            lore.add(ru.realite.classes.util.Components.c("&7" + line));
                        }
                    }
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(
                            classIdKey,
                            org.bukkit.persistence.PersistentDataType.STRING,
                            def.id.name());

                    item.setItemMeta(meta);
                }

                inventory.setItem(startSlot + i, item);
            }
        }
    }

    public Inventory create() {
        return inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public ClassId extractClassId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;

        String raw = meta.getPersistentDataContainer().get(classIdKey, PersistentDataType.STRING);
        return ClassId.fromString(raw);
    }
}
