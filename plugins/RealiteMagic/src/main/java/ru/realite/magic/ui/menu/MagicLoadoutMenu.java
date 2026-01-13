package ru.realite.magic.ui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.SetSlotResult;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicLoadoutMenu implements InventoryHolder, MagicUiMenu {

    private static final int SIZE = 27;
    private static final int SLOT_BACK = 18;
    private static final int SLOT_CLEAR = 22;

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiScreenRegistry screenRegistry;
    private final Map<Integer, BiConsumer<Player, InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;

    public MagicLoadoutMenu(MagicService magicService,
                            SpellRegistry spellRegistry,
                            PlayerSpellService playerSpellService,
                            MagicMessages messages,
                            UiScreenRegistry screenRegistry) {
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.screenRegistry = Objects.requireNonNull(screenRegistry, "screenRegistry");
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent event) {
        BiConsumer<Player, InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(player, event);
        }
    }

    private Inventory build(Player player) {
        inventory = Bukkit.createInventory(this, SIZE, messages.msg("ui.magic.loadout.title"));
        fill(player);
        return inventory;
    }

    private void fill(Player player) {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        actions.clear();

        int activeSlot = playerSpellService.getActiveSlot(player.getUniqueId());
        int slotCount = magicService.slotCount();
        for (int slot = 1; slot <= slotCount; slot++) {
            int inventorySlot = slot - 1;
            String spellId = playerSpellService.getSlot(player.getUniqueId(), slot).orElse(null);
            SpellDefinition spell = spellId == null ? null : spellRegistry.get(spellId);
            inventory.setItem(inventorySlot, createSlotItem(slot, spell, spellId, activeSlot));
            int selectionSlot = slot;
            actions.put(inventorySlot, (viewer, event) ->
                    screenRegistry.open(viewer, "magic.spells:slot=" + selectionSlot));
        }

        setButton(SLOT_BACK, Material.ARROW, messages.msg("ui.common.back"), null,
                (viewer, event) -> screenRegistry.open(viewer, "magic.spells"));
        setClearButton(player, activeSlot);
    }

    private void setClearButton(Player player, int activeSlot) {
        List<Component> lore = new ArrayList<>();
        lore.add(messages.msg("ui.magic.loadout.clear.hint", "slot", String.valueOf(activeSlot)));
        setButton(SLOT_CLEAR, Material.BARRIER, messages.msg("ui.magic.loadout.clear.name"), lore,
                (viewer, event) -> handleClearSlot(viewer, activeSlot));
    }

    private void handleClearSlot(Player player, int activeSlot) {
        SetSlotResult result = playerSpellService.setSlot(player.getUniqueId(), activeSlot, null);
        if (result instanceof SetSlotResult.Fail fail) {
            player.sendMessage(messages.msg(fail.reasonKey(), "slot", String.valueOf(activeSlot)));
            return;
        }
        player.sendMessage(messages.msg("magic.slot.clear.ok"));
        new MagicLoadoutMenu(magicService, spellRegistry, playerSpellService, messages, screenRegistry)
                .open(player);
    }

    private ItemStack createSlotItem(int slot, SpellDefinition spell, String spellId, int activeSlot) {
        boolean isActive = slot == activeSlot;
        ItemStack item = new ItemStack(resolveMaterial(spell));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(messages.msg("ui.magic.loadout.slot.name", "slot", String.valueOf(slot)));
        List<Component> lore = new ArrayList<>();
        if (spell != null) {
            meta.displayName(messages.msg("ui.magic.loadout.slot.filled",
                    "slot", String.valueOf(slot),
                    "spell", messages.raw(spell.nameKey())));
            lore.add(messages.msg("ui.magic.loadout.slot.spell", "spell", messages.raw(spell.nameKey())));
            if (spell.iconCustomModelData() != null) {
                meta.setCustomModelData(spell.iconCustomModelData());
            }
        } else if (spellId != null) {
            lore.add(messages.msg("ui.magic.loadout.slot.unknown", "spell", spellId));
        } else {
            lore.add(messages.msg("ui.magic.loadout.slot.empty"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        if (isActive) {
            lore.add(messages.msg("ui.magic.loadout.slot.active"));
        }
        lore.add(messages.msg("ui.magic.loadout.slot.hint"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material resolveMaterial(SpellDefinition spell) {
        if (spell == null) {
            return Material.GRAY_STAINED_GLASS_PANE;
        }
        return spell.iconMaterial();
    }

    private void setButton(int slot,
                           Material material,
                           Component name,
                           List<Component> lore,
                           BiConsumer<Player, InventoryClickEvent> action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }
}
