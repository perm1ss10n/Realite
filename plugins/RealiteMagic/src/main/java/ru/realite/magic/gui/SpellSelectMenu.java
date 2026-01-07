package ru.realite.magic.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class SpellSelectMenu implements InventoryHolder {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final SpellRegistry spellRegistry;
    private final MagicMessages messages;
    private final MagicService magicService;
    private final NamespacedKey spellIdKey;
    private final JavaPlugin plugin;
    private Inventory inventory;

    public SpellSelectMenu(JavaPlugin plugin,
                           SpellRegistry spellRegistry,
                           MagicMessages messages,
                           MagicService magicService) {
        this.plugin = plugin;
        this.spellRegistry = spellRegistry;
        this.messages = messages;
        this.magicService = magicService;
        this.spellIdKey = new NamespacedKey(plugin, "spell_id");
    }

    public void open(Player player) {
        player.openInventory(create(player));
    }

    public Inventory create(Player player) {
        int size = menuSize();
        String title = messages.raw("magic.menu.title");
        inventory = Bukkit.createInventory(this, size, LEGACY.deserialize(title));
        fill(player);
        return inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public String extractSpellId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(spellIdKey, PersistentDataType.STRING);
    }

    private void fill(Player player) {
        if (inventory == null) {
            return;
        }
        inventory.clear();

        String activeSpellId = magicService.getActiveSpellId(player);
        boolean showUnavailable = plugin.getConfig().getBoolean("menu.spellSelect.showUnavailable", false);
        List<SpellDefinition> spells = new ArrayList<>(spellRegistry.all());
        spells.sort(Comparator.comparing(SpellDefinition::id));

        List<Integer> autoSlots = menuSlots();
        int autoIndex = 0;
        for (SpellDefinition spell : spells) {
            boolean available = magicService.meetsRequirements(player, spell);
            if (!available && !showUnavailable) {
                continue;
            }
            Integer targetSlot = spell.guiSlot();
            if (targetSlot == null) {
                while (autoIndex < autoSlots.size()) {
                    int candidate = autoSlots.get(autoIndex++);
                    if (candidate < 0 || candidate >= inventory.getSize()) {
                        continue;
                    }
                    if (inventory.getItem(candidate) != null) {
                        continue;
                    }
                    targetSlot = candidate;
                    break;
                }
            }
            if (targetSlot == null || targetSlot < 0 || targetSlot >= inventory.getSize()) {
                continue;
            }
            if (inventory.getItem(targetSlot) != null) {
                continue;
            }
            inventory.setItem(targetSlot, createSpellItem(spell, activeSpellId, available));
        }
    }

    private ItemStack createSpellItem(SpellDefinition spell, String activeSpellId, boolean available) {
        ItemStack item = new ItemStack(spell.iconMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (spell.iconCustomModelData() != null) {
            meta.setCustomModelData(spell.iconCustomModelData());
        }
        meta.displayName(LEGACY.deserialize(messages.raw(spell.nameKey())));

        List<Component> lore = new ArrayList<>();
        String desc = messages.raw(spell.descKey());
        if (desc != null && !desc.isBlank()) {
            lore.add(LEGACY.deserialize(desc));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize(messages.raw("magic.menu.mana-line")
                .replace("{mana}", formatNumber(spell.mana(), "menu.spellSelect.manaFormat", "0.0"))));
        lore.add(LEGACY.deserialize(messages.raw("magic.menu.cooldown-line")
                .replace("{cooldown}", formatNumber(spell.cooldownTicks() / 20.0,
                        "menu.spellSelect.cooldownFormat", "0.0"))));

        if (!available) {
            lore.add(Component.empty());
            lore.add(LEGACY.deserialize(messages.raw("magic.menu.unavailable")));
        } else if (spell.id().equals(activeSpellId)) {
            lore.add(Component.empty());
            lore.add(LEGACY.deserialize(messages.raw("magic.menu.selected")));
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(spellIdKey, PersistentDataType.STRING, spell.id());
        item.setItemMeta(meta);
        return item;
    }

    private int menuSize() {
        int size = plugin.getConfig().getInt("menu.spellSelect.size", 0);
        if (size <= 0) {
            size = 54;
        }
        int rows = Math.max(1, (size + 8) / 9);
        return Math.min(rows, 6) * 9;
    }

    private List<Integer> menuSlots() {
        List<Integer> slots = plugin.getConfig().getIntegerList("menu.spellSelect.slots");
        if (slots == null || slots.isEmpty()) {
            return fallbackSlots();
        }
        return filterSlots(slots);
    }

    private List<Integer> filterSlots(List<Integer> slots) {
        List<Integer> filtered = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int size = inventory == null ? menuSize() : inventory.getSize();
        for (Integer slot : slots) {
            if (slot == null) {
                continue;
            }
            if (slot < 0 || slot >= size) {
                continue;
            }
            if (!seen.add(slot)) {
                continue;
            }
            filtered.add(slot);
        }
        return filtered;
    }

    private List<Integer> fallbackSlots() {
        List<Integer> slots = new ArrayList<>();
        int size = inventory == null ? menuSize() : inventory.getSize();
        for (int i = 0; i < size; i++) {
            slots.add(i);
        }
        return slots;
    }

    private String formatNumber(double value, String configKey, String fallbackPattern) {
        String pattern = plugin.getConfig().getString(configKey, fallbackPattern);
        if (pattern == null || pattern.isBlank()) {
            pattern = fallbackPattern;
        }
        DecimalFormat format;
        try {
            format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ex) {
            format = new DecimalFormat(fallbackPattern, DecimalFormatSymbols.getInstance(Locale.US));
        }
        return format.format(value);
    }
}
