package ru.realite.magic.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    private Inventory inventory;

    public SpellSelectMenu(JavaPlugin plugin,
                           SpellRegistry spellRegistry,
                           MagicMessages messages,
                           MagicService magicService) {
        this.spellRegistry = spellRegistry;
        this.messages = messages;
        this.magicService = magicService;
        this.spellIdKey = new NamespacedKey(plugin, "spell_id");
    }

    public void open(Player player) {
        player.openInventory(create(player));
    }

    public Inventory create(Player player) {
        int size = menuSize(player);
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
        List<SpellDefinition> spells = new ArrayList<>(spellRegistry.all());
        spells.sort(Comparator.comparing(SpellDefinition::id));

        int slot = 0;
        for (SpellDefinition spell : spells) {
            if (!magicService.meetsRequirements(player, spell)) {
                continue;
            }
            if (slot >= inventory.getSize()) {
                break;
            }
            inventory.setItem(slot++, createSpellItem(spell, activeSpellId));
        }
    }

    private ItemStack createSpellItem(SpellDefinition spell, String activeSpellId) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(LEGACY.deserialize(messages.raw(spell.nameKey())));

        List<Component> lore = new ArrayList<>();
        String desc = messages.raw(spell.descKey());
        if (desc != null && !desc.isBlank()) {
            lore.add(LEGACY.deserialize(desc));
        }
        lore.add(Component.empty());
        lore.add(LEGACY.deserialize(messages.raw("magic.menu.mana-line")
                .replace("{mana}", format(spell.mana()))));
        lore.add(LEGACY.deserialize(messages.raw("magic.menu.cooldown-line")
                .replace("{cooldown}", formatCooldown(spell.cooldownTicks()))));

        if (spell.id().equals(activeSpellId)) {
            lore.add(Component.empty());
            lore.add(LEGACY.deserialize(messages.raw("magic.menu.selected")));
        }

        meta.lore(lore);
        meta.getPersistentDataContainer().set(spellIdKey, PersistentDataType.STRING, spell.id());
        item.setItemMeta(meta);
        return item;
    }

    private int menuSize(Player player) {
        int count = 0;
        for (SpellDefinition spell : spellRegistry.all()) {
            if (magicService.meetsRequirements(player, spell)) {
                count++;
            }
        }
        count = Math.max(1, count);
        int rows = Math.max(1, (count + 8) / 9);
        rows = Math.min(rows, 6);
        return rows * 9;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatCooldown(long cooldownTicks) {
        double seconds = cooldownTicks / 20.0;
        return String.format(Locale.US, "%.1f", seconds);
    }
}
