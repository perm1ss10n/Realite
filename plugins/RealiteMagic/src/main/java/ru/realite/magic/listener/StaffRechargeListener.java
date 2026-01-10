package ru.realite.magic.listener;

import java.util.Objects;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.service.StaffChargeService;
import ru.realite.magic.service.StaffChargeService.StaffCharges;
import ru.realite.magic.service.StaffChargeService.StaffItem;

public final class StaffRechargeListener implements Listener {

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final ItemsBridge itemsBridge;
    private final StaffChargeService staffChargeService;

    public StaffRechargeListener(JavaPlugin plugin,
                                 MagicMessages messages,
                                 ItemsBridge itemsBridge,
                                 StaffChargeService staffChargeService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
        this.staffChargeService = Objects.requireNonNull(staffChargeService, "staffChargeService");
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
        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("staff.recharge.enabled", true)) {
            return;
        }
        String itemId = config.getString("staff.recharge.itemId");
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        int addCharges = config.getInt("staff.recharge.addCharges", 0);
        if (addCharges <= 0) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack staffStack = player.getInventory().getItemInMainHand();
        if (!staffChargeService.isStaff(staffStack)) {
            return;
        }
        ItemStack crystal = player.getInventory().getItemInOffHand();
        if (!itemsBridge.isItem(crystal, itemId)) {
            return;
        }
        StaffCharges charges = staffChargeService.readCharges(staffStack);
        if (charges.max() <= 0 || charges.current() >= charges.max()) {
            return;
        }
        int newCharges = Math.min(charges.max(), charges.current() + addCharges);
        int added = newCharges - charges.current();
        if (added <= 0) {
            return;
        }
        staffChargeService.writeCharges(player, new StaffItem(staffStack, false), newCharges);
        consumeOffhand(player, crystal);
        player.sendMessage(messages.msg("magic.staff.recharged", "add", String.valueOf(added)));
        event.setCancelled(true);
    }

    private void consumeOffhand(Player player, ItemStack stack) {
        if (stack == null || player == null) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            player.getInventory().setItemInOffHand(null);
        } else {
            stack.setAmount(amount - 1);
            player.getInventory().setItemInOffHand(stack);
        }
    }
}
