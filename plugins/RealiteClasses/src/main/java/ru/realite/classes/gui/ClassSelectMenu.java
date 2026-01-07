package ru.realite.classes.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.HiddenClassGate;
import ru.realite.classes.service.HiddenClassGateResult;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;

import java.util.List;

public class ClassSelectMenu implements InventoryHolder {

    public static final int SIZE = 27;

    private final ClassConfigRepository classConfig;
    private final ClassLoreRepository classLore;
    private final HiddenClassGate hiddenClassGate;
    private Inventory inventory;
    private final NamespacedKey classIdKey;

    public ClassSelectMenu(JavaPlugin plugin,
                           ClassConfigRepository classConfig,
                           ClassLoreRepository classLore,
                           HiddenClassGate hiddenClassGate) {
        this.classConfig = classConfig;
        this.classLore = classLore;
        this.hiddenClassGate = hiddenClassGate;
        this.classIdKey = new NamespacedKey(plugin, "class_id");
        this.inventory = Bukkit.createInventory(
                this,
                SIZE,
                ru.realite.classes.util.Components.c("&6Классы"));
    }

    private void fill(Player player) {
        inventory.clear();

        // стабильный порядок по enum
        var ids = ru.realite.classes.model.ClassId.values();
        int index = 0;
        int total = ids.length;
        int rows = SIZE / 9;

        for (int row = 0; row < rows && index < total; row++) {
            int rowStart = row * 9;

            int remaining = total - index;
            int countInRow = Math.min(9, remaining);

            // центрируем содержимое строки
            int startSlot = rowStart + (9 - countInRow) / 2;

            for (int i = 0; i < countInRow; i++) {
                ClassId id = ids[index++];
                var def = classConfig.get(id);
                var loreDef = classLore != null ? classLore.get(id) : null;

                boolean hiddenLocked = false;
                HiddenClassGateResult gateResult = null;
                boolean hidden = (def != null && def.hidden) || (loreDef != null && loreDef.hiddenEnabled);
                if (hidden) {
                    gateResult = hiddenClassGate != null ? hiddenClassGate.check(player, id) : null;
                    hiddenLocked = gateResult != null && !gateResult.available();
                }

                Material icon = loreDef != null ? loreDef.icon : null;
                if (icon == null && def != null) {
                    icon = def.icon;
                }

                ItemStack item = new ItemStack(hiddenLocked
                        ? (loreDef != null && loreDef.lockedIcon != null ? loreDef.lockedIcon : Material.BARRIER)
                        : (icon != null ? icon : Material.PAPER));
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (hiddenLocked) {
                        String lockedName = loreDef != null ? loreDef.lockedName : null;
                        if (lockedName == null || lockedName.isBlank()) {
                            lockedName = "&8???";
                        }
                        meta.displayName(ru.realite.classes.util.Components.c(lockedName));
                    } else {
                        String displayName = loreDef != null ? loreDef.displayName : null;
                        if (displayName == null || displayName.isBlank()) {
                            displayName = def != null ? def.name : id.name();
                        }
                        meta.displayName(ru.realite.classes.util.Components.c(displayName));
                    }

                    List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                    if (hiddenLocked) {
                        List<String> lockedLore = loreDef != null ? loreDef.lockedLore : List.of();
                        if (lockedLore.isEmpty()) {
                            lore.add(ru.realite.classes.util.Components.c("&7..."));
                        } else {
                            for (String line : lockedLore) {
                                lore.add(ru.realite.classes.util.Components.c(line));
                            }
                        }
                    } else {
                        List<String> lines = loreDef != null ? loreDef.lore : List.of();
                        if ((lines == null || lines.isEmpty()) && def != null) {
                            lines = def.lore;
                        }
                        if (lines != null) {
                            for (String line : lines) {
                                lore.add(ru.realite.classes.util.Components.c(line));
                            }
                        }
                    }
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(
                            classIdKey,
                            PersistentDataType.STRING,
                            id.name());

                    item.setItemMeta(meta);
                }

                inventory.setItem(startSlot + i, item);
            }
        }
    }

    public Inventory create(Player player) {
        // ✅ ВАЖНО: обновляем содержимое каждый раз, чтобы /class reload сразу отражался в GUI
        this.inventory = Bukkit.createInventory(
                this,
                SIZE,
                ru.realite.classes.util.Components.c("&6Классы"));
        fill(player);
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
