package ru.realite.magic.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.items.service.ItemService;

public final class MagicInteractListener implements Listener {

    private static final String PERMISSION_USE = "realite.magic.use";
    private static final long WRONG_ITEM_WARN_COOLDOWN_MS = 2000L;
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final SpellRegistry spellRegistry;
    private final MagicMessages messages;
    private final Map<UUID, Long> wrongItemWarns = new HashMap<>();

    public MagicInteractListener(MagicService magicService,
                                 PlayerSpellService playerSpellService,
                                 SpellRegistry spellRegistry,
                                 MagicMessages messages) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.spellRegistry = spellRegistry;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.getPlayer().hasPermission(PERMISSION_USE)) {
            event.getPlayer().sendMessage(messages.msg("magic.command.errors.no_permission"));
            return;
        }

        var playerId = event.getPlayer().getUniqueId();
        var selectedSpellId = playerSpellService.getSelected(playerId).orElse(null);
        if (selectedSpellId == null) {
            event.getPlayer().sendMessage(messages.msg("magic.cast.no_selected"));
            return;
        }
        SpellDefinition spell = spellRegistry.find(selectedSpellId).orElse(null);
        if (spell == null) {
            playerSpellService.clearSelected(playerId);
            event.getPlayer().sendMessage(messages.msg("magic.spell.unknown", "spell", selectedSpellId));
            return;
        }

        if (!hasCastItem(event.getPlayer(), spell)) {
            return;
        }

        if (!magicService.hasRequiredFocus(event.getPlayer())) {
            event.getPlayer().sendMessage(messages.msg("magic.error.need_focus"));
            return;
        }

        magicService.cast(event.getPlayer(), spell);
    }

    private boolean hasCastItem(org.bukkit.entity.Player player, SpellDefinition spell) {
        String castItemId = spell.castItemId();
        if (castItemId == null || castItemId.isBlank()) {
            return true;
        }
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            return true;
        }
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (itemService.isItem(inHand, castItemId)) {
            return true;
        }
        warnWrongItem(player, itemService, castItemId);
        return false;
    }

    private void warnWrongItem(org.bukkit.entity.Player player, ItemService itemService, String castItemId) {
        long now = System.currentTimeMillis();
        Long lastWarn = wrongItemWarns.get(player.getUniqueId());
        if (lastWarn != null && now - lastWarn < WRONG_ITEM_WARN_COOLDOWN_MS) {
            return;
        }
        wrongItemWarns.put(player.getUniqueId(), now);
        String itemName = resolveItemName(itemService, castItemId);
        player.sendMessage(messages.msg("magic.cast.wrong_item", "item", itemName));
    }

    private String resolveItemName(ItemService itemService, String castItemId) {
        try {
            ItemStack stack = itemService.create(castItemId, 1);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component displayName = meta.displayName();
                if (displayName != null && !displayName.equals(Component.empty())) {
                    return LEGACY.serialize(displayName);
                }
            }
        } catch (IllegalArgumentException ex) {
            return castItemId;
        }
        return castItemId;
    }

    private ItemService resolveItemService() {
        RegisteredServiceProvider<ItemService> provider =
                Bukkit.getServicesManager().getRegistration(ItemService.class);
        return provider != null ? provider.getProvider() : null;
    }
}
