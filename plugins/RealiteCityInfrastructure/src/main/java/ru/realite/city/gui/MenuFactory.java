package ru.realite.city.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotOwnerType;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.storage.PlotRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MenuFactory {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final int MAIN_SIZE = 27;
    private static final int SELECTION_SIZE = 27;
    private static final int PLOTS_SIZE = 54;
    private static final int ACTIONS_SIZE = 27;
    private static final int PLOTS_PER_PAGE = 45;

    private final CityMessages messages;
    private final CityAreaSelectionService selectionService;
    private final PlotRepository plotRepository;
    private final NamespacedKey actionKey;
    private final NamespacedKey plotIdKey;

    public MenuFactory(JavaPlugin plugin,
                       CityMessages messages,
                       CityAreaSelectionService selectionService,
                       PlotRepository plotRepository) {
        this.messages = messages;
        this.selectionService = selectionService;
        this.plotRepository = plotRepository;
        this.actionKey = new NamespacedKey(plugin, "gui_action");
        this.plotIdKey = new NamespacedKey(plugin, "gui_plot_id");
    }

    public Inventory adminMain() {
        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(MenuType.ADMIN_MAIN),
                MAIN_SIZE,
                messages.get("gui.title.admin_main", "Admin"));

        inventory.setItem(10, actionItem(Material.COMPASS, "gui.btn.selection", "open_selection"));
        inventory.setItem(12, actionItem(Material.PAPER, "gui.btn.plots", "open_plots"));
        inventory.setItem(14, actionItem(Material.MAP, "gui.btn.regions", "noop"));
        inventory.setItem(16, actionItem(Material.BOOK, "gui.btn.help", "noop"));

        return inventory;
    }

    public Inventory adminSelection(Player player) {
        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(MenuType.ADMIN_SELECTION),
                SELECTION_SIZE,
                messages.get("gui.title.admin_selection", "Selection"));

        inventory.setItem(10, actionItem(Material.LIME_DYE, "gui.selection.pos1.set", "selection_pos1"));
        inventory.setItem(12, actionItem(Material.LIGHT_BLUE_DYE, "gui.selection.pos2.set", "selection_pos2"));
        inventory.setItem(14, actionItem(Material.BARRIER, "gui.selection.clear", "selection_clear"));
        inventory.setItem(16, selectionStatusItem(player));
        inventory.setItem(22, actionItem(Material.ARROW, "gui.btn.back", "open_main"));

        return inventory;
    }

    public Inventory adminPlots(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(MenuType.ADMIN_PLOTS),
                PLOTS_SIZE,
                messages.get("gui.title.admin_plots", "Plots"));

        List<Plot> plots = new ArrayList<>(plotRepository.findAll());
        plots.sort(Comparator.comparingInt(Plot::number));

        int maxPage = Math.max(0, (plots.size() - 1) / PLOTS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        int startIndex = safePage * PLOTS_PER_PAGE;
        int endIndex = Math.min(plots.size(), startIndex + PLOTS_PER_PAGE);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Plot plot = plots.get(i);
            inventory.setItem(slot++, plotItem(plot));
        }

        inventory.setItem(45, actionItem(Material.ARROW, pageLabel("&e<"), "plots_prev"));
        inventory.setItem(49, pageIndicatorItem(safePage, maxPage));
        inventory.setItem(53, actionItem(Material.ARROW, pageLabel("&e>"), "plots_next"));
        inventory.setItem(52, actionItem(Material.BARRIER, "gui.btn.back", "open_main"));

        return inventory;
    }

    public int clampPlotsPage(int page) {
        int total = plotRepository.findAll().size();
        int maxPage = Math.max(0, (total - 1) / PLOTS_PER_PAGE);
        return Math.max(0, Math.min(page, maxPage));
    }

    public Inventory adminPlotActions(Player player, Plot plot, boolean confirmDelete) {
        Inventory inventory = Bukkit.createInventory(
                new MenuHolder(MenuType.ADMIN_PLOT_ACTIONS),
                ACTIONS_SIZE,
                messages.get("gui.title.admin_plot_actions", "Plot"));

        inventory.setItem(10, plotInfoItem(plot));
        inventory.setItem(12, deleteItem(confirmDelete));
        inventory.setItem(14, teleportItem());
        inventory.setItem(16, stubItem(Material.PLAYER_HEAD, "Set owner (player)", "plot_set_owner_player"));
        inventory.setItem(19, stubItem(Material.WRITABLE_BOOK, "Set owner (guild)", "plot_set_owner_guild"));
        inventory.setItem(22, actionItemKey(Material.ARROW, "gui.btn.back", "open_plots"));

        return inventory;
    }

    public Optional<String> extractAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        return Optional.ofNullable(action);
    }

    public Optional<String> extractPlotId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        String plotId = meta.getPersistentDataContainer().get(plotIdKey, PersistentDataType.STRING);
        return Optional.ofNullable(plotId);
    }

    private ItemStack actionItemKey(Material material, String key, String action) {
        String label = messages.getRaw(key, key);
        return actionItem(material, label, action);
    }

    private ItemStack actionItem(Material material, String label, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize(label));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack selectionStatusItem(Player player) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.get("gui.selection.status", "Selection"));
            List<Component> lore = new ArrayList<>();
            var selection = selectionService.getSelection(player.getUniqueId()).orElse(null);
            lore.add(LEGACY.deserialize("&7Pos1: " + formatLocation(selection == null ? null : selection.pos1())));
            lore.add(LEGACY.deserialize("&7Pos2: " + formatLocation(selection == null ? null : selection.pos2())));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack plotItem(Plot plot) {
        ItemStack item = new ItemStack(plot.type() == null ? Material.PAPER : switch (plot.type()) {
            case HOME -> Material.GRASS_BLOCK;
            case SHOP -> Material.CHEST;
        });
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("&e#" + plot.number() + " &7(" + plot.id() + ")"));
            List<Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize("&7Type: " + plot.type()));
            lore.add(LEGACY.deserialize("&7Owner: " + ownerName(plot.ownerType(), plot.ownerId())));
            lore.add(LEGACY.deserialize("&7World: " + plot.world()));
            lore.add(LEGACY.deserialize("&7Price: " + plot.price()));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_plot_actions");
            meta.getPersistentDataContainer().set(plotIdKey, PersistentDataType.STRING, plot.id());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack plotInfoItem(Plot plot) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("&e#" + plot.number() + " &7(" + plot.id() + ")"));
            List<Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize("&7Type: " + plot.type()));
            lore.add(LEGACY.deserialize("&7Owner: " + ownerName(plot.ownerType(), plot.ownerId())));
            lore.add(LEGACY.deserialize("&7World: " + plot.world()));
            lore.add(LEGACY.deserialize("&7Price: " + plot.price()));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack deleteItem(boolean confirmDelete) {
        String label = confirmDelete ? "&cConfirm delete" : "&cDelete plot";
        ItemStack item = actionItem(Material.TNT, pageLabel(label), confirmDelete ? "plot_delete_confirm" : "plot_delete");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize(confirmDelete ? "&7Click again to delete" : "&7Click to delete"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack teleportItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("&bTeleport to plot"));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "plot_teleport");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack stubItem(Material material, String label, String action) {
        ItemStack item = actionItem(material, pageLabel("&7" + label), action);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize("&7TBD"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pageIndicatorItem(int page, int maxPage) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LEGACY.deserialize("&ePage " + (page + 1) + "/" + (maxPage + 1)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String pageLabel(String label) {
        return label == null ? "" : label;
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null) {
            return "-";
        }
        String world = location.getWorld() == null ? "?" : location.getWorld().getName();
        return world + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String ownerName(PlotOwnerType ownerType, UUID ownerId) {
        if (ownerId == null || ownerType == null) {
            return "none";
        }
        if (ownerType == PlotOwnerType.PLAYER) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(ownerId);
            String name = player.getName();
            return name == null ? ownerId.toString() : name;
        }
        return "guild:" + ownerId;
    }
}
