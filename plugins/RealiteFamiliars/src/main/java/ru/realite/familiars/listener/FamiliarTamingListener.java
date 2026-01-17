package ru.realite.familiars.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.event.FamiliarTamedEvent;
import ru.realite.familiars.integration.items.ItemsBridge;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.TameResult;
import ru.realite.familiars.ui.FamiliarActionBarService;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class FamiliarTamingListener implements Listener {

    private static final NamespacedKey TAMING_TAG_KEY = new NamespacedKey("realite", "realite_familiar_tag");
    private static final NamespacedKey TYPE_KEY = new NamespacedKey("realite", "familiarTypeId");
    private static final String EXPECTED_ITEM_ID = "realite:familiar_taming_tag";

    private final FamiliarService service;
    private final Messages messages;
    private final ItemsBridge itemsBridge;
    private final Logger logger;
    private final FamiliarActionBarService actionBar;

    public FamiliarTamingListener(FamiliarService service,
                                  Messages messages,
                                  ItemsBridge itemsBridge,
                                  Logger logger,
                                  FamiliarActionBarService actionBar) {
        this.service = service;
        this.messages = messages;
        this.itemsBridge = itemsBridge;
        this.logger = logger;
        this.actionBar = actionBar;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        handleTame(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> handleTame(event.getPlayer());
            default -> {
            }
        }
    }

    private void handleTame(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        if (!isTamingTag(stack)) {
            return;
        }

        String typeId = getTypeId(stack);
        if (typeId == null) {
            player.sendMessage(messages.get("taming.missing-type"));
            debug("Missing familiarTypeId for player " + player.getName());
            return;
        }

        TameResult result = service.tame(player, typeId);
        if (!result.allowed()) {
            if (result.result().reasons().isEmpty()) {
                player.sendMessage(messages.get("taming.failure"));
                debug("Taming failed without reasons for " + player.getName() + " type=" + typeId);
                return;
            }
            if (actionBar != null) {
                actionBar.sendForReasons(player, result.result().reasons());
            }
            for (String reason : result.result().reasons()) {
                player.sendMessage(messages.get("taming.failure-reason", Map.of("reason", reason)));
                debug("Taming failed for " + player.getName() + " type=" + typeId + " reason=" + reason);
            }
            return;
        }

        if (result.instance() != null) {
            Bukkit.getPluginManager().callEvent(new FamiliarTamedEvent(player, result.instance()));
        }
        consumeTag(player, stack);
        player.sendMessage(messages.get("taming.success", Map.of("type", typeId)));
        if (actionBar != null) {
            actionBar.send(player, "actionbar.tamed");
        }
    }

    private boolean isTamingTag(ItemStack stack) {
        if (itemsBridge != null) {
            if (itemsBridge.isAvailable()) {
                Optional<String> itemId = itemsBridge.getItemId(stack);
                if (itemId.isEmpty() || !EXPECTED_ITEM_ID.equalsIgnoreCase(itemId.get())) {
                    return false;
                }
                Optional<Integer> tagValue = itemsBridge.readInt(stack, TAMING_TAG_KEY.getKey());
                if (tagValue.isPresent()) {
                    return tagValue.get() > 0;
                }
            } else {
                itemsBridge.getItemId(stack);
            }
        }
        PersistentDataContainer container = stack.getItemMeta() != null
                ? stack.getItemMeta().getPersistentDataContainer()
                : null;
        if (container == null) {
            return false;
        }
        Integer value = container.get(TAMING_TAG_KEY, PersistentDataType.INTEGER);
        return value != null && value > 0;
    }

    private String getTypeId(ItemStack stack) {
        if (itemsBridge != null && itemsBridge.isAvailable()) {
            Optional<String> typeId = itemsBridge.readString(stack, TYPE_KEY.getKey());
            if (typeId.isPresent() && !typeId.get().isBlank()) {
                return typeId.get();
            }
        }
        if (stack.getItemMeta() == null) {
            return null;
        }
        PersistentDataContainer container = stack.getItemMeta().getPersistentDataContainer();
        String typeId = container.get(TYPE_KEY, PersistentDataType.STRING);
        if (typeId == null || typeId.isBlank()) {
            return null;
        }
        return typeId;
    }

    private void consumeTag(Player player, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        player.getInventory().setItemInMainHand(stack);
    }

    private void debug(String message) {
        if (logger != null) {
            logger.fine("[Familiars] " + message);
        }
    }
}
