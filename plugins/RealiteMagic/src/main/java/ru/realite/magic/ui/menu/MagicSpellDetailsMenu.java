package ru.realite.magic.ui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;

public final class MagicSpellDetailsMenu implements InventoryHolder, MagicUiMenu {

    private static final int SIZE = 27;
    private static final int SLOT_SPELL = 13;
    private static final int SLOT_BACK = 18;

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
        inventory = Bukkit.createInventory(this, SIZE, messages.msg("magic.ui.spell.details.title", "spell", name));
        fill(player);
        return inventory;
    }

    private void fill(Player player) {
        inventory.clear();
        actions.clear();

        inventory.setItem(SLOT_SPELL, createSpellItem(player));
        setButton(SLOT_BACK, Material.ARROW, messages.msg("magic.ui.common.back"), null,
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
        lore.add(messages.msg("magic.spell.lore.mana", "mana", formatNumber(spell.mana())));
        lore.add(messages.msg("magic.spell.lore.cooldown", "seconds", formatNumber(spell.cooldownTicks() / 20.0)));
        lore.add(messages.msg("magic.spell.lore.range", "range", formatNumber(spell.range())));
        lore.add(messages.msg("magic.spell.lore.damage", "damage", formatNumber(spell.damage())));

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

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
