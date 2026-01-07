package ru.realite.classes.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import ru.realite.classes.util.Messages;

import java.util.List;

public class ClassSelectMenu implements InventoryHolder {

    public static final int SIZE = 27;

    private final ClassConfigRepository classConfig;
    private final HiddenClassGate hiddenClassGate;
    private final Messages messages;
    private Inventory inventory;
    private final NamespacedKey classIdKey;
    private final LegacyComponentSerializer legacySection = LegacyComponentSerializer.legacySection();

    public ClassSelectMenu(JavaPlugin plugin, ClassConfigRepository classConfig, HiddenClassGate hiddenClassGate, Messages messages) {
        this.classConfig = classConfig;
        this.hiddenClassGate = hiddenClassGate;
        this.messages = messages;
        this.classIdKey = new NamespacedKey(plugin, "class_id");
        this.inventory = Bukkit.createInventory(
                this,
                SIZE,
                Component.text("Выбор класса"));
    }

    private void fill(Player player) {
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

                boolean hiddenLocked = false;
                HiddenClassGateResult gateResult = null;
                if (def.hidden) {
                    gateResult = hiddenClassGate != null ? hiddenClassGate.check(player, def.id) : null;
                    hiddenLocked = gateResult != null && !gateResult.available();
                }

                ItemStack item = new ItemStack(hiddenLocked
                        ? Material.BARRIER
                        : (def.icon != null ? def.icon : Material.PAPER));
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (hiddenLocked) {
                        meta.displayName(ru.realite.classes.util.Components.c("&6???"));
                    } else {
                        meta.displayName(ru.realite.classes.util.Components.c("&6" + def.name));
                    }

                    List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                    if (hiddenLocked) {
                        appendLockedLore(def.id, lore, gateResult);
                    } else if (def.lore != null) {
                        for (String line : def.lore) {
                            lore.add(ru.realite.classes.util.Components.c("&7" + line));
                        }
                    }
                    meta.lore(lore);

                    meta.getPersistentDataContainer().set(
                            classIdKey,
                            PersistentDataType.STRING,
                            def.id.name());

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
                Component.text("Выбор класса"));
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

    private void appendLockedLore(ClassId classId, List<Component> lore, HiddenClassGateResult gateResult) {
        lore.add(legacySection.deserialize(messages.get("class-locked")));
        if (gateResult == null || gateResult.reasonKey() == null) {
            return;
        }

        String reasonKey = gateResult.reasonKey();
        if (reasonKey.equals("class-locked-quest")) {
            String questId = hiddenClassGate != null ? hiddenClassGate.requiredQuestId(classId) : null;
            String questLabel = questId != null ? questId : "-";
            lore.add(legacySection.deserialize(messages.format("class-locked-quest", java.util.Map.of(
                    "quest", questLabel))));
        } else if (reasonKey.equals("class-locked-evolution")) {
            lore.add(legacySection.deserialize(messages.get("class-locked-evolution")));
            lore.add(legacySection.deserialize(messages.format("class-locked-requirements", java.util.Map.of(
                    "req", hiddenClassGate != null ? hiddenClassGate.describeEvolutionRequirement(classId) : "-"))));
        } else if (reasonKey.equals("class-locked-both")) {
            lore.add(legacySection.deserialize(messages.get("class-locked-both")));
            String questId = hiddenClassGate != null ? hiddenClassGate.requiredQuestId(classId) : null;
            String questLabel = questId != null ? questId : "-";
            lore.add(legacySection.deserialize(messages.format("class-locked-quest", java.util.Map.of(
                    "quest", questLabel))));
            lore.add(legacySection.deserialize(messages.format("class-locked-requirements", java.util.Map.of(
                    "req", hiddenClassGate != null ? hiddenClassGate.describeEvolutionRequirement(classId) : "-"))));
        }
    }
}
