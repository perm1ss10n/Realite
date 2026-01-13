package ru.realite.magic.ui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.service.EquipSpellFailure;
import ru.realite.magic.service.EquipSpellResult;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.UnequipSpellResult;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRequirements;

public final class MagicSpellDetailsMenu implements InventoryHolder, MagicUiMenu {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int SIZE = 27;
    private static final int SLOT_SPELL = 13;
    private static final int SLOT_BACK = 18;
    private static final int SLOT_ACTION = 26;

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiScreenRegistry screenRegistry;
    private final SpellDefinition spell;
    private final Map<Integer, BiConsumer<Player, InventoryClickEvent>> actions = new java.util.HashMap<>();
    private Inventory inventory;

    public MagicSpellDetailsMenu(MagicService magicService,
                                 PlayerSpellService playerSpellService,
                                 MagicMessages messages,
                                 UiScreenRegistry screenRegistry,
                                 SpellDefinition spell) {
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.screenRegistry = Objects.requireNonNull(screenRegistry, "screenRegistry");
        this.spell = Objects.requireNonNull(spell, "spell");
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
        String name = messages.raw(spell.nameKey());
        inventory = Bukkit.createInventory(this, SIZE, messages.msg("ui.magic.spell.details.title", "spell", name));
        fill(player);
        return inventory;
    }

    private void fill(Player player) {
        inventory.clear();
        actions.clear();

        inventory.setItem(SLOT_SPELL, createSpellItem(player));
        setActionButtons(player);
        setButton(SLOT_BACK, Material.ARROW, messages.msg("ui.common.back"), null,
                (viewer, event) -> screenRegistry.open(viewer, "magic.spells"));
    }

    private ItemStack createSpellItem(Player player) {
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
        if (spell.mana() > 0) {
            lore.add(messages.msg("magic.spell.lore.mana", "mana", formatNumber(spell.mana())));
        }
        if (spell.cooldownTicks() > 0) {
            lore.add(messages.msg("magic.spell.lore.cooldown",
                    "seconds", formatNumber(spell.cooldownTicks() / 20.0)));
        }
        if (spell.moneyCost() > 0) {
            lore.add(messages.msg("ui.magic.spell.cost", "cost", formatNumber(spell.moneyCost())));
        }

        addRequirements(player, lore);

        boolean learned = playerSpellService.hasSpell(player.getUniqueId(), spell.id());
        CheckResult result = magicService.checkRequirements(player, spell);
        if (result instanceof CheckResult.Ok) {
            lore.add(messages.msg("magic.ui.spells.status.available"));
        } else if (result instanceof CheckResult.Fail fail) {
            lore.add(messages.msg("magic.ui.spells.status.unavailable"));
            String reason = formatRequirementReason(fail);
            if (reason != null && !reason.isBlank()) {
                lore.add(messages.msg("magic.ui.spells.status.unavailable_reason", "reason", reason));
            }
        }
        if (learned) {
            lore.add(messages.msg("magic.ui.spells.status.learned"));
        } else {
            lore.add(messages.msg("magic.ui.spells.status.not_learned"));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        Integer equippedSlot = resolveSlot(player.getUniqueId());
        if (equippedSlot != null) {
            lore.add(messages.msg("magic.ui.spells.status.equipped", "slot", String.valueOf(equippedSlot)));
            if (equippedSlot == playerSpellService.getActiveSlot(player.getUniqueId())) {
                lore.add(messages.msg("magic.ui.spells.status.active_slot"));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void addRequirements(Player player, List<Component> lore) {
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        lore.add(messages.msg("ui.magic.requirements.title"));
        if (requirements.classId() != null && !requirements.classId().isBlank()) {
            lore.add(messages.msg("ui.magic.requirements.class",
                    "value", displayClassName(requirements.classId())));
        }
        if (requirements.evolutionId() != null && !requirements.evolutionId().isBlank()) {
            lore.add(messages.msg("ui.magic.requirements.evolution",
                    "value", displayEvolutionName(requirements.evolutionId())));
        }
        if (requirements.requiredItemId() != null && !requirements.requiredItemId().isBlank()) {
            lore.add(messages.msg("ui.magic.requirements.item",
                    "value", displayItemName(requirements.requiredItemId())));
        }

        CheckResult availability = magicService.checkRequirements(player, spell);
        if (availability instanceof CheckResult.Fail fail) {
            String reason = formatRequirementReason(fail);
            lore.add(messages.msg("ui.magic.requirements.not_met", "reason", reason == null ? "" : reason));
        }
    }

    private void setActionButtons(Player player) {
        UUID playerId = player.getUniqueId();
        boolean learned = playerSpellService.hasSpell(playerId, spell.id());
        CheckResult availability = magicService.checkRequirements(player, spell);
        Integer equippedSlot = resolveSlot(playerId);

        if (equippedSlot != null) {
            setButton(SLOT_ACTION, Material.BARRIER, messages.msg("ui.magic.action.unequip"), null,
                    (viewer, event) -> handleUnequip(viewer));
            return;
        }
        if (learned && availability instanceof CheckResult.Ok) {
            setButton(SLOT_ACTION, Material.EMERALD, messages.msg("ui.magic.action.equip"), null,
                    (viewer, event) -> handleEquip(viewer));
        }
    }

    private void handleEquip(Player player) {
        EquipSpellResult result = magicService.equipSpell(player, spell);
        if (result instanceof EquipSpellResult.Ok ok) {
            player.sendMessage(messages.msg("ui.magic.success.equipped", "slot", String.valueOf(ok.slot())));
            reopen(player);
            return;
        }
        if (result instanceof EquipSpellResult.Fail fail) {
            if (fail.reason() == EquipSpellFailure.NOT_AVAILABLE) {
                String reason = fail.requirement() == null ? "" : formatRequirementReason(fail.requirement());
                player.sendMessage(messages.msg("ui.magic.error.not_available", "reason", reason));
                return;
            }
            player.sendMessage(messages.msg("ui.magic.error.cannot_equip"));
        }
    }

    private void handleUnequip(Player player) {
        UnequipSpellResult result = magicService.unequipSpell(player, spell);
        if (result instanceof UnequipSpellResult.Ok) {
            player.sendMessage(messages.msg("ui.magic.success.unequipped"));
            reopen(player);
            return;
        }
        player.sendMessage(messages.msg("ui.magic.error.cannot_equip"));
    }

    private void reopen(Player player) {
        new MagicSpellDetailsMenu(magicService, playerSpellService, messages, screenRegistry, spell).open(player);
    }

    private Integer resolveSlot(UUID playerId) {
        String needle = spell.id().toLowerCase(Locale.ROOT);
        for (int slot = 1; slot <= 9; slot++) {
            String spellId = playerSpellService.getSlot(playerId, slot).orElse(null);
            if (spellId != null && spellId.toLowerCase(Locale.ROOT).equals(needle)) {
                return slot;
            }
        }
        return null;
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

    private String formatRequirementReason(CheckResult.Fail fail) {
        String raw = messages.raw(fail.reasonKey());
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        for (var entry : fail.placeholders().entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }

    private String displayClassName(String classId) {
        return LEGACY.serialize(magicService.classesBridge().displayClassName(classId));
    }

    private String displayEvolutionName(String evolutionId) {
        return LEGACY.serialize(magicService.classesBridge().displayEvolutionName(evolutionId));
    }

    private String displayItemName(String itemId) {
        if (magicService.itemsBridge() instanceof NoopItemsBridge) {
            return itemId;
        }
        return LEGACY.serialize(magicService.itemsBridge().displayName(itemId));
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
