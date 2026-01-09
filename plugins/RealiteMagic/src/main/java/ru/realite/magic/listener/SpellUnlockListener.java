package ru.realite.magic.listener;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;
import ru.realite.magic.spell.SpellDefinition;

public final class SpellUnlockListener implements Listener {

    private static final String SCROLL_ITEM_ID = "spell_scroll";
    private static final String GRIMOIRE_ITEM_ID = "spell_grimoire";
    private static final String SCROLL_PREFIX = "spell_scroll_";
    private static final String GRIMOIRE_PREFIX = "spell_grimoire_";

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final NamespacedKey spellIdKey;

    public SpellUnlockListener(MagicService magicService,
                               PlayerSpellService playerSpellService,
                               MagicMessages messages) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
        this.spellIdKey = new NamespacedKey("realite", "spell_id");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        Optional<String> itemId = magicService.itemsBridge().getItemId(item);
        if (itemId.isEmpty()) {
            return;
        }
        String normalizedItemId = itemId.get().toLowerCase(Locale.ROOT);
        if (!isUnlockItem(normalizedItemId)) {
            return;
        }
        event.setCancelled(true);
        String spellId = extractSpellId(item, normalizedItemId);
        if (spellId == null) {
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.invalid_scroll"));
            return;
        }
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.invalid_scroll"));
            return;
        }
        if (playerSpellService.hasSpell(event.getPlayer().getUniqueId(), spell.id())) {
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.already",
                    "spell", displaySpellName(spell)));
            return;
        }
        CheckResult checkResult = magicService.checkRequirements(event.getPlayer(), spell);
        if (checkResult instanceof CheckResult.Fail fail) {
            String reason = messages.raw(fail.reasonKey(), fail.placeholders());
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.denied",
                    "spell", displaySpellName(spell),
                    "reason", reason));
            return;
        }
        UnlockResult result = playerSpellService.unlock(event.getPlayer().getUniqueId(), spell.id(), SpellUnlockSource.ITEM);
        if (result instanceof UnlockResult.Fail) {
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.invalid_scroll"));
            return;
        }
        UnlockResult.Ok ok = (UnlockResult.Ok) result;
        if (ok.alreadyLearned()) {
            event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.already",
                    "spell", displaySpellName(spell)));
            return;
        }
        consumeItem(event.getPlayer().getInventory().getItem(event.getHand()),
                event.getHand(),
                event.getPlayer().getInventory());
        event.getPlayer().sendMessage(messages.msg("magic.spell.unlock.from_item.ok",
                "spell", displaySpellName(spell)));
    }

    private boolean isUnlockItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }
        return itemId.equals(SCROLL_ITEM_ID)
                || itemId.equals(GRIMOIRE_ITEM_ID)
                || itemId.startsWith(SCROLL_PREFIX)
                || itemId.startsWith(GRIMOIRE_PREFIX);
    }

    private String extractSpellId(ItemStack item, String itemId) {
        String fromPdc = readSpellId(item);
        if (fromPdc != null && !fromPdc.isBlank()) {
            return fromPdc;
        }
        if (itemId == null) {
            return null;
        }
        if (itemId.startsWith(SCROLL_PREFIX)) {
            return trimSpellId(itemId.substring(SCROLL_PREFIX.length()));
        }
        if (itemId.startsWith(GRIMOIRE_PREFIX)) {
            return trimSpellId(itemId.substring(GRIMOIRE_PREFIX.length()));
        }
        return null;
    }

    private String readSpellId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(spellIdKey, PersistentDataType.STRING);
    }

    private String trimSpellId(String spellId) {
        if (spellId == null) {
            return null;
        }
        String trimmed = spellId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String displaySpellName(SpellDefinition spell) {
        if (spell == null) {
            return "";
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        String raw = messages.raw(nameKey);
        return raw.isBlank() ? spell.id() : raw;
    }

    private void consumeItem(ItemStack item, EquipmentSlot slot, PlayerInventory inventory) {
        if (item == null) {
            return;
        }
        int amount = item.getAmount();
        if (amount <= 1) {
            inventory.setItem(slot, null);
            return;
        }
        item.setAmount(amount - 1);
        inventory.setItem(slot, item);
    }
}
