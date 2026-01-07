package ru.realite.magic.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
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
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.spell.SpellRequirements;

public final class SpellSelectMenu implements InventoryHolder {

    private final SpellRegistry spellRegistry;
    private final MagicMessages messages;
    private final MagicService magicService;
    private final NamespacedKey spellIdKey;
    private final NamespacedKey menuKey;
    private final NamespacedKey menuActionKey;
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
        this.spellIdKey = new NamespacedKey("realite", "spell_id");
        this.menuKey = new NamespacedKey("realite", "menu");
        this.menuActionKey = new NamespacedKey(plugin, "menu_action");
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    public Inventory build(Player player) {
        int size = menuSize();
        inventory = Bukkit.createInventory(this, size, messages.msg(menuTitleKey()));
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
        String menuId = meta.getPersistentDataContainer().get(menuKey, PersistentDataType.STRING);
        if (menuId == null || !menuId.equalsIgnoreCase("spell_select")) {
            return null;
        }
        return meta.getPersistentDataContainer().get(spellIdKey, PersistentDataType.STRING);
    }

    public String extractMenuAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(menuActionKey, PersistentDataType.STRING);
    }

    public boolean isMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String menuId = meta.getPersistentDataContainer().get(menuKey, PersistentDataType.STRING);
        return menuId != null && menuId.equalsIgnoreCase("spell_select");
    }

    public boolean isMenuTitle(Component title) {
        return Objects.equals(title, messages.msg(menuTitleKey()));
    }

    public boolean isCloseButton(ItemStack item) {
        String action = extractMenuAction(item);
        return action != null && action.equalsIgnoreCase("close");
    }

    public boolean shouldCloseOnSelect() {
        return plugin.getConfig().getBoolean("menu.spellSelect.closeOnSelect", true);
    }

    public String requirementReason(SpellDefinition spell) {
        if (spell == null) {
            return null;
        }
        return buildRequirementReason(spell.requirements());
    }

    private void fill(Player player) {
        if (inventory == null) {
            return;
        }
        inventory.clear();

        String selectedSpellId = magicService.getSelectedSpellId(player);
        List<SpellDefinition> spells = new ArrayList<>(spellRegistry.all());
        spells.sort(Comparator.comparing(SpellDefinition::id));

        List<Integer> autoSlots = menuSlots();
        int autoIndex = 0;
        int placed = 0;
        for (SpellDefinition spell : spells) {
            boolean available = magicService.meetsRequirements(player, spell);
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
            inventory.setItem(targetSlot, createSpellItem(spell, selectedSpellId, available));
            placed++;
        }

        if (placed == 0) {
            placeNoSpellsItem();
        }
        applyFiller();
        applyButtons();
    }

    private ItemStack createSpellItem(SpellDefinition spell, String selectedSpellId, boolean available) {
        ItemStack item = new ItemStack(spell.iconMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (spell.iconCustomModelData() != null) {
            meta.setCustomModelData(spell.iconCustomModelData());
        }
        meta.displayName(messages.msg(spell.nameKey()));

        List<Component> lore = new ArrayList<>();
        String desc = messages.raw(spell.descKey());
        if (desc != null && !desc.isBlank()) {
            lore.add(messages.msg("magic.spell.lore.description", "desc", desc));
        }
        lore.add(messages.msg("magic.spell.lore.mana",
                "mana", formatNumber(spell.mana(), "menu.spellSelect.manaFormat", "0.0")));
        lore.add(messages.msg("magic.spell.lore.cooldown",
                "seconds", formatNumber(spell.cooldownTicks() / 20.0, "menu.spellSelect.cooldownFormat", "0.0")));
        lore.add(messages.msg("magic.spell.lore.range",
                "range", formatNumber(spell.range(), "menu.spellSelect.rangeFormat", "0.0")));
        lore.add(messages.msg("magic.spell.lore.damage",
                "damage", formatNumber(spell.damage(), "menu.spellSelect.damageFormat", "0.0")));

        if (spell.id().equals(selectedSpellId)) {
            lore.add(messages.msg("magic.spell.lore.selected"));
        }
        if (!available) {
            String reason = buildRequirementReason(spell.requirements());
            if (reason != null && !reason.isBlank()) {
                lore.add(messages.msg("magic.spell.lore.locked_reason", "reason", reason));
            }
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(spellIdKey, PersistentDataType.STRING, spell.id());
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.STRING, "spell_select");
        item.setItemMeta(meta);
        return item;
    }

    private void applyFiller() {
        if (inventory == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("menu.spellSelect.filler.enabled", false)) {
            return;
        }
        Material material = resolveMaterial("menu.spellSelect.filler.material", "BLACK_STAINED_GLASS_PANE");
        String nameKey = plugin.getConfig().getString("menu.spellSelect.filler.nameKey", "");
        ItemStack filler = createMenuItem(material, nameKey, null);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void applyButtons() {
        if (inventory == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("menu.spellSelect.buttons.close.enabled", false)) {
            return;
        }
        int slot = plugin.getConfig().getInt("menu.spellSelect.buttons.close.slot", -1);
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        Material material = resolveMaterial("menu.spellSelect.buttons.close.material", "BARRIER");
        String nameKey = plugin.getConfig().getString("menu.spellSelect.buttons.close.nameKey", "");
        ItemStack item = createMenuItem(material, nameKey, "close");
        inventory.setItem(slot, item);
    }

    private ItemStack createMenuItem(Material material, String nameKey, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (nameKey != null && !nameKey.isBlank()) {
            meta.displayName(messages.msg(nameKey));
        } else {
            meta.displayName(Component.empty());
        }
        if (action != null && !action.isBlank()) {
            meta.getPersistentDataContainer().set(menuActionKey, PersistentDataType.STRING, action);
        }
        meta.getPersistentDataContainer().set(menuKey, PersistentDataType.STRING, "spell_select");
        item.setItemMeta(meta);
        return item;
    }

    private void placeNoSpellsItem() {
        if (inventory == null) {
            return;
        }
        int slot = inventory.getSize() / 2;
        Material material = resolveMaterial("menu.spellSelect.noSpells.material", "PAPER");
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.msg("magic.menu.spell_select.no_spells"));
            meta.getPersistentDataContainer().set(menuKey, PersistentDataType.STRING, "spell_select");
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
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

    private String menuTitleKey() {
        String key = plugin.getConfig().getString("menu.spellSelect.titleKey");
        if (key == null || key.isBlank()) {
            key = "magic.menu.spell_select.title";
        }
        return key;
    }

    private Material resolveMaterial(String configKey, String fallbackName) {
        String name = plugin.getConfig().getString(configKey, fallbackName);
        if (name == null || name.isBlank()) {
            name = fallbackName;
        }
        Material material = Material.matchMaterial(name.trim());
        if (material == null) {
            material = Material.matchMaterial(fallbackName);
        }
        return material == null ? Material.PAPER : material;
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

    private String buildRequirementReason(SpellRequirements requirements) {
        if (requirements == null || requirements.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            String raw = messages.raw("magic.spell.lore.requirements.class");
            parts.add(raw.replace("{class}", classId));
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            String raw = messages.raw("magic.spell.lore.requirements.evolution");
            parts.add(raw.replace("{evolution}", evolutionId));
        }
        if (parts.isEmpty()) {
            return null;
        }
        String separator = messages.raw("magic.spell.lore.requirements.separator");
        if (separator == null || separator.isBlank()) {
            separator = " ";
        }
        return String.join(separator, parts);
    }
}
