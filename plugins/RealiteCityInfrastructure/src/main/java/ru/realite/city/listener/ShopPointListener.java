package ru.realite.city.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotMemberRole;
import ru.realite.city.model.ShopPoint;
import ru.realite.city.service.ShopPointService;
import ru.realite.city.service.ShopRentService;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopPointListener implements Listener {

    private static final String ADMIN_PERMISSION = "realite.city.admin";
    private static final int TOGGLE_SLOT = 4;

    private final ShopPointService shopPointService;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;
    private final CityMessages messages;
    private final ShopRentService rentService;
    private final Map<UUID, MenuState> menus = new ConcurrentHashMap<>();

    public ShopPointListener(
            ShopPointService shopPointService,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            CityMessages messages,
            ShopRentService rentService
    ) {
        this.shopPointService = shopPointService;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.messages = messages;
        this.rentService = rentService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Optional<ShopPoint> pointOptional = shopPointService.findExact(block.getLocation());
        if (pointOptional.isEmpty()) {
            return;
        }
        ShopPoint point = pointOptional.get();
        Player player = event.getPlayer();
        Optional<Plot> plotOptional = plotRepository.findById(point.plotId());
        if (plotOptional.isEmpty()) {
            return;
        }
        Plot plot = plotOptional.get();
        boolean isAdmin = player.hasPermission(ADMIN_PERMISSION);
        boolean isOwner = plot.ownerUuid() != null && plot.ownerUuid().equals(player.getUniqueId());
        boolean isTrusted = plotMemberRepository
                .findRole(plot.id(), player.getUniqueId())
                .map(role -> role == PlotMemberRole.TRUSTED)
                .orElse(false);
        if (!isAdmin && !isOwner && !isTrusted) {
            if (!point.enabled()) {
                messages.send(player, "city.shop.point.disabled", "");
                return;
            }
            messages.send(player, "city.shop.point.customer", "");
            return;
        }
        if (rentService != null && rentService.isRentEnabled()
                && rentService.isCommerceBlocked(plot, System.currentTimeMillis())) {
            messages.send(player, "city.shop.rent.blocked", "", Map.of("id", plot.id()));
            return;
        }
        openMenu(player, point);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        MenuState state = menus.get(player.getUniqueId());
        if (state == null || event.getView().getTopInventory() != state.inventory()) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != TOGGLE_SLOT) {
            return;
        }
        Optional<ShopPoint> pointOptional = shopPointService.findById(state.shopPointId());
        if (pointOptional.isEmpty()) {
            player.closeInventory();
            return;
        }
        ShopPoint point = pointOptional.get();
        boolean newState = !point.enabled();
        shopPointService.setEnabled(point, newState);
        updateToggleItem(state.inventory(), newState);
        messages.send(player, newState ? "city.shop.point.enabled" : "city.shop.point.disabled-by-owner", "");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        menus.remove(player.getUniqueId());
    }

    private void openMenu(Player player, ShopPoint point) {
        Component title = messages.get("city.shop.point.menu.title", "");
        Inventory inventory = Bukkit.createInventory(player, 9, title);
        updateToggleItem(inventory, point.enabled());
        menus.put(player.getUniqueId(), new MenuState(point.id(), inventory));
        player.openInventory(inventory);
    }

    private void updateToggleItem(Inventory inventory, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.get(enabled
                ? "city.shop.point.menu.toggle.enabled"
                : "city.shop.point.menu.toggle.disabled", ""));
        item.setItemMeta(meta);
        inventory.setItem(TOGGLE_SLOT, item);
    }

    private record MenuState(String shopPointId, Inventory inventory) {
    }
}
