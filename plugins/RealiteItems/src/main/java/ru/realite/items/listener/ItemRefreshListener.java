package ru.realite.items.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import ru.realite.items.RealiteItemsPlugin;
import ru.realite.items.service.ItemService;

public final class ItemRefreshListener implements Listener {

    private final RealiteItemsPlugin plugin;
    private final ItemService itemService;

    public ItemRefreshListener(RealiteItemsPlugin plugin, ItemService itemService) {
        this.plugin = plugin;
        this.itemService = itemService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.isRefreshOnJoin()) {
            return;
        }
        var inventory = event.getPlayer().getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            if (itemService.getItemId(stack).isEmpty()) {
                continue;
            }
            inventory.setItem(i, itemService.render(stack));
        }
    }
}
