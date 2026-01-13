package ru.realite.magic.ui.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.core.api.ui.UiPage;
import ru.realite.core.api.ui.UiPaginationService;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellActionReason;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicSpellbookMenu implements InventoryHolder, MagicUiMenu {

    private static final int SIZE = 54;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_PAGE = 49;
    private static final int SLOT_NEXT = 53;

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiPaginationService paginationService;
    private final UiScreenRegistry screenRegistry;
    private final int requestedPage;
    private final Map<Integer, BiConsumer<Player, InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;
    private int currentPage;

    public MagicSpellbookMenu(MagicService magicService,
                              SpellRegistry spellRegistry,
                              PlayerSpellService playerSpellService,
                              MagicMessages messages,
                              UiPaginationService paginationService,
                              UiScreenRegistry screenRegistry,
                              int page) {
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.paginationService = Objects.requireNonNull(paginationService, "paginationService");
        this.screenRegistry = Objects.requireNonNull(screenRegistry, "screenRegistry");
        this.requestedPage = Math.max(0, page);
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
        int slot = event.getRawSlot();
        BiConsumer<Player, InventoryClickEvent> action = actions.get(slot);
        if (action != null) {
            action.accept(player, event);
        }
    }

    private Inventory build(Player player) {
        inventory = Bukkit.createInventory(this, SIZE, messages.msg("magic.ui.spells.title"));
        fill(player);
        return inventory;
    }

    private void fill(Player player) {
        if (inventory == null) {
            return;
        }
        inventory.clear();
        actions.clear();

        List<SpellDefinition> spells = new ArrayList<>(spellRegistry.all());
        spells.sort(Comparator.comparing(SpellDefinition::id));

        List<Integer> slots = contentSlots();
        UiPage<SpellDefinition> page = paginationService.paginate(spells, requestedPage, slots.size());
        currentPage = page.page();

        Map<String, Integer> slotMap = resolveSlots(player.getUniqueId());
        int activeSlot = playerSpellService.getActiveSlot(player.getUniqueId());

        int index = 0;
        for (SpellDefinition spell : page.items()) {
            int slot = slots.get(index++);
            boolean learned = playerSpellService.hasSpell(player.getUniqueId(), spell.id());
            CheckResult result = magicService.checkRequirements(player, spell);
            Integer equippedSlot = slotMap.get(spell.id().toLowerCase(Locale.ROOT));
            inventory.setItem(slot, createSpellItem(spell, learned, result, equippedSlot, activeSlot));
            actions.put(slot, (viewer, event) -> handleSpellClick(viewer, event, spell));
        }

        if (spells.isEmpty()) {
            placeEmptyItem();
        }

        applyNavigation(page);
    }

    private void handleSpellClick(Player player, InventoryClickEvent event, SpellDefinition spell) {
        if (event.getClick() == ClickType.SHIFT_LEFT) {
            handleQuickEquip(player, spell);
            return;
        }
        if (event.isLeftClick()) {
            screenRegistry.open(player, "magic.spell.details:" + spell.id());
        }
    }

    private void handleQuickEquip(Player player, SpellDefinition spell) {
        if (!playerSpellService.hasSpell(player.getUniqueId(), spell.id())) {
            player.sendMessage(messages.msg("magic.spell.select.not_learned",
                    "spell", messages.raw(spell.nameKey())));
            return;
        }
        CheckResult result = magicService.checkRequirements(player, spell);
        if (result instanceof CheckResult.Fail fail) {
            String reason = formatRequirementReason(fail);
            player.sendMessage(messages.msg("magic.command.spell.locked",
                    "reason", reason == null ? "" : reason));
            return;
        }
        SelectResult selection = playerSpellService.select(player.getUniqueId(), spell.id());
        if (selection instanceof SelectResult.Ok) {
            player.sendMessage(messages.msg("magic.spell.select.ok",
                    "spell", messages.raw(spell.nameKey())));
            return;
        }
        if (selection instanceof SelectResult.Fail fail) {
            SpellActionReason reason = fail.reason();
            if (reason == SpellActionReason.NOT_LEARNED) {
                player.sendMessage(messages.msg("magic.spell.select.not_learned",
                        "spell", messages.raw(spell.nameKey())));
                return;
            }
            if (reason == SpellActionReason.UNKNOWN_SPELL) {
                player.sendMessage(messages.msg("magic.spell.unknown",
                        "spell", spell.id()));
                return;
            }
            player.sendMessage(messages.msg("magic.command.errors.no_permission"));
        }
    }

    private void applyNavigation(UiPage<SpellDefinition> page) {
        setButton(SLOT_PAGE, Material.PAPER,
                messages.msg("magic.ui.spells.page",
                        "current", String.valueOf(page.page() + 1),
                        "total", String.valueOf(page.totalPages())),
                null,
                null);

        if (page.hasPrevious()) {
            setButton(SLOT_PREV, Material.ARROW, messages.msg("magic.ui.common.prev"), null,
                    (player, event) -> new MagicSpellbookMenu(magicService, spellRegistry, playerSpellService,
                            messages, paginationService, screenRegistry, currentPage - 1).open(player));
        }
        if (page.hasNext()) {
            setButton(SLOT_NEXT, Material.ARROW, messages.msg("magic.ui.common.next"), null,
                    (player, event) -> new MagicSpellbookMenu(magicService, spellRegistry, playerSpellService,
                            messages, paginationService, screenRegistry, currentPage + 1).open(player));
        }
    }

    private ItemStack createSpellItem(SpellDefinition spell,
                                      boolean learned,
                                      CheckResult availability,
                                      Integer equippedSlot,
                                      int activeSlot) {
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

        if (availability instanceof CheckResult.Ok) {
            lore.add(messages.msg("magic.ui.spells.status.available"));
        } else if (availability instanceof CheckResult.Fail fail) {
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

        if (equippedSlot != null) {
            lore.add(messages.msg("magic.ui.spells.status.equipped", "slot", String.valueOf(equippedSlot)));
            if (equippedSlot == activeSlot) {
                lore.add(messages.msg("magic.ui.spells.status.active_slot"));
            }
        }

        lore.add(messages.msg("magic.ui.spells.hint.details"));
        lore.add(messages.msg("magic.ui.spells.hint.quick_equip"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void placeEmptyItem() {
        int slot = SIZE / 2;
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.msg("magic.ui.spells.empty"));
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
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

    private List<Integer> contentSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            slots.add(i);
        }
        return slots;
    }

    private Map<String, Integer> resolveSlots(UUID playerId) {
        Map<String, Integer> map = new HashMap<>();
        for (int slot = 1; slot <= 9; slot++) {
            playerSpellService.getSlot(playerId, slot)
                    .ifPresent(spellId -> map.put(spellId.toLowerCase(Locale.ROOT), slot));
        }
        return map;
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
}
