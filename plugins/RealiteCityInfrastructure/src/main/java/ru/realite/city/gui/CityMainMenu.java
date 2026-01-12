package ru.realite.city.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotOwnerType;
import ru.realite.city.model.PlotType;
import ru.realite.city.service.ShopPointService;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CityMainMenu {

    private static final int SIZE = 54;
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    private static CityMessages messages;
    private static PlotRepository plotRepository;
    private static PlotMemberRepository plotMemberRepository;
    private static ShopPointService shopPointService;

    private CityMainMenu() {
    }

    public static void init(CityMessages messages,
                            PlotRepository plotRepository,
                            PlotMemberRepository plotMemberRepository,
                            ShopPointService shopPointService) {
        CityMainMenu.messages = messages;
        CityMainMenu.plotRepository = plotRepository;
        CityMainMenu.plotMemberRepository = plotMemberRepository;
        CityMainMenu.shopPointService = shopPointService;
    }

    public static void open(Player player) {
        if (messages == null || plotRepository == null || plotMemberRepository == null || shopPointService == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(new CityMenuHolder(buildActions(plotRepository,
                plotMemberRepository, shopPointService)), SIZE, messages.get("city.gui.title", "City"));
        fillBackground(inventory, messages);
        renderItems(player, inventory, messages, plotRepository, plotMemberRepository, shopPointService);
        player.openInventory(inventory);
    }

    private static void renderItems(Player player,
                                    Inventory inventory,
                                    CityMessages messages,
                                    PlotRepository plotRepository,
                                    PlotMemberRepository plotMemberRepository,
                                    ShopPointService shopPointService) {
        PlotContext plotContext = PlotContext.from(player, plotRepository, plotMemberRepository, shopPointService);
        for (CityMenuItemDefinition definition : definitions()) {
            boolean available = definition.isAvailable(player, plotContext);
            List<Component> lore = buildLore(messages, definition, available, player, plotContext);
            Component name = messages.get("city.gui.item." + definition.id() + ".name", definition.id());
            ItemStack item = CityMenuItemFactory.create(definition.material(), name, lore, available);
            inventory.setItem(definition.slot(), item);
        }
    }

    private static List<Component> buildLore(CityMessages messages,
                                             CityMenuItemDefinition definition,
                                             boolean available,
                                             Player player,
                                             PlotContext plotContext) {
        List<Component> lore = new ArrayList<>();
        String lore1 = messages.getRaw("city.gui.item." + definition.id() + ".lore1", "");
        if (!lore1.isBlank()) {
            lore.add(messages.get("city.gui.item." + definition.id() + ".lore1", ""));
        }
        String lore2 = messages.getRaw("city.gui.item." + definition.id() + ".lore2", "");
        if (!lore2.isBlank()) {
            lore.add(messages.get("city.gui.item." + definition.id() + ".lore2", ""));
        }
        if (available) {
            lore.add(messages.get("city.gui.available", ""));
        } else {
            String reasonKey = resolveUnavailableKey(definition, player, plotContext);
            lore.add(CityMenuItemFactory.unavailableReason(messages, reasonKey,
                    messages.getRaw("city.gui.unavailable.condition", "")));
        }
        return lore;
    }

    private static String resolveUnavailableKey(CityMenuItemDefinition definition,
                                                Player player,
                                                PlotContext plotContext) {
        if (definition.permission() != null && !definition.permission().isBlank()
                && !player.hasPermission(definition.permission())) {
            return "city.gui.unavailable.no-permission";
        }
        if (definition.condition() != null && !definition.condition().test(plotContext)) {
            Function<CityMessages, String> reasonKey = definition.unavailableReasonKey();
            if (reasonKey != null) {
                return reasonKey.apply(messages);
            }
            if (!plotContext.hasPlot()) {
                return "city.gui.unavailable.no-plot";
            }
        }
        return "city.gui.unavailable.condition";
    }

    private static void fillBackground(Inventory inventory, CityMessages messages) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, FILLER);
        }
        Component title = messages.get("city.gui.title", "City").color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
        Component hint = messages.get("city.gui.hint", "").color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
        ItemStack header = CityMenuItemFactory.create(Material.NAME_TAG, title, List.of(hint), true);
        inventory.setItem(4, header);
    }

    private static List<CityMenuItemDefinition> definitions() {
        List<CityMenuItemDefinition> items = new ArrayList<>();
        items.add(new CityMenuItemDefinition(
                "help",
                10,
                Material.BOOK,
                null,
                null,
                null,
                player -> player.performCommand("city")));
        items.add(new CityMenuItemDefinition(
                "plot-nearby",
                19,
                Material.MAP,
                null,
                null,
                null,
                player -> player.performCommand("city plot nearby")));
        items.add(new CityMenuItemDefinition(
                "plot-info",
                20,
                Material.PAPER,
                null,
                PlotContext::hasPlot,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot info")));
        items.add(new CityMenuItemDefinition(
                "plot-buy",
                21,
                Material.EMERALD,
                null,
                PlotContext::isInsidePlot,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot buy")));
        items.add(new CityMenuItemDefinition(
                "plot-members",
                23,
                Material.PLAYER_HEAD,
                null,
                PlotContext::canManageMembers,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot members")));
        items.add(new CityMenuItemDefinition(
                "plot-addmember",
                24,
                Material.LIME_DYE,
                null,
                PlotContext::canManageMembers,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot addmember")));
        items.add(new CityMenuItemDefinition(
                "plot-removemember",
                25,
                Material.RED_DYE,
                null,
                PlotContext::canManageMembers,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot removemember")));
        items.add(new CityMenuItemDefinition(
                "plot-release",
                28,
                Material.BARRIER,
                null,
                PlotContext::isOwnerOrAdmin,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot release")));
        items.add(new CityMenuItemDefinition(
                "market-list",
                31,
                Material.CHEST,
                null,
                null,
                null,
                player -> player.performCommand("city market")));
        items.add(new CityMenuItemDefinition(
                "market-near",
                32,
                Material.COMPASS,
                null,
                null,
                null,
                player -> player.performCommand("city market near")));
        items.add(new CityMenuItemDefinition(
                "market-search",
                33,
                Material.WRITABLE_BOOK,
                null,
                null,
                null,
                player -> player.performCommand("city market search")));
        items.add(new CityMenuItemDefinition(
                "market-category",
                34,
                Material.MAP,
                null,
                null,
                null,
                player -> player.performCommand("city market category")));
        items.add(new CityMenuItemDefinition(
                "market-info",
                35,
                Material.PAPER,
                null,
                null,
                null,
                player -> player.performCommand("city market info")));
        items.add(new CityMenuItemDefinition(
                "market-goto",
                36,
                Material.ENDER_PEARL,
                "realite.city.market.tp",
                null,
                null,
                player -> player.performCommand("city market goto")));
        items.add(new CityMenuItemDefinition(
                "market-hub",
                37,
                Material.NETHER_STAR,
                "realite.city.market.tp",
                null,
                null,
                player -> player.performCommand("city market hub")));
        items.add(new CityMenuItemDefinition(
                "shop-setup",
                40,
                Material.ANVIL,
                null,
                PlotContext::isNearOwnedShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop setup")));
        items.add(new CityMenuItemDefinition(
                "shop-open",
                41,
                Material.LIME_DYE,
                null,
                PlotContext::isNearOwnedShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop open")));
        items.add(new CityMenuItemDefinition(
                "shop-close",
                42,
                Material.RED_DYE,
                null,
                PlotContext::isNearOwnedShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop close")));
        items.add(new CityMenuItemDefinition(
                "shop-set",
                43,
                Material.WRITABLE_BOOK,
                null,
                PlotContext::isNearOwnedShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop set")));
        items.add(new CityMenuItemDefinition(
                "shop-list",
                44,
                Material.BOOKSHELF,
                null,
                PlotContext::isOwnerOrAdmin,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop list")));
        items.add(new CityMenuItemDefinition(
                "shop-info",
                45,
                Material.PAPER,
                null,
                PlotContext::isNearShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop info")));
        items.add(new CityMenuItemDefinition(
                "shop-remove",
                46,
                Material.BARRIER,
                null,
                PlotContext::isNearOwnedShopPoint,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop remove")));
        items.add(new CityMenuItemDefinition(
                "shop-rent-status",
                47,
                Material.CLOCK,
                null,
                PlotContext::isOwnerOrAdmin,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop rent status")));
        items.add(new CityMenuItemDefinition(
                "shop-rent-pay",
                48,
                Material.GOLD_INGOT,
                null,
                PlotContext::isOwnerOrAdmin,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city shop rent pay")));
        items.add(new CityMenuItemDefinition(
                "area-list",
                13,
                Material.PAPER,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city area list")));
        items.add(new CityMenuItemDefinition(
                "area-wand",
                14,
                Material.WOODEN_AXE,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city area wand")));
        items.add(new CityMenuItemDefinition(
                "area-create",
                15,
                Material.LIME_DYE,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city area create")));
        items.add(new CityMenuItemDefinition(
                "area-delete",
                16,
                Material.RED_DYE,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city area delete")));
        items.add(new CityMenuItemDefinition(
                "plot-list",
                12,
                Material.BOOK,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot list")));
        items.add(new CityMenuItemDefinition(
                "plot-create",
                11,
                Material.OAK_SIGN,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot create")));
        items.add(new CityMenuItemDefinition(
                "plot-delete",
                17,
                Material.BARRIER,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot delete")));
        items.add(new CityMenuItemDefinition(
                "plot-transfer",
                18,
                Material.NAME_TAG,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot transfer")));
        items.add(new CityMenuItemDefinition(
                "plot-sell",
                26,
                Material.EMERALD_BLOCK,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot sell")));
        items.add(new CityMenuItemDefinition(
                "plot-accept",
                27,
                Material.LIME_WOOL,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot accept")));
        items.add(new CityMenuItemDefinition(
                "plot-goto",
                29,
                Material.ENDER_PEARL,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot goto")));
        items.add(new CityMenuItemDefinition(
                "plot-setowner",
                30,
                Material.WRITABLE_BOOK,
                "realite.city.admin",
                null,
                null,
                player -> player.performCommand("city plot setowner")));
        items.add(new CityMenuItemDefinition(
                "plot-trust",
                22,
                Material.GREEN_DYE,
                null,
                PlotContext::canManageMembers,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot trust")));
        items.add(new CityMenuItemDefinition(
                "plot-untrust",
                38,
                Material.GRAY_DYE,
                null,
                PlotContext::canManageMembers,
                messages -> "city.gui.unavailable.no-plot",
                player -> player.performCommand("city plot untrust")));
        return items;
    }

    private static Map<Integer, CityMenuAction> buildActions(
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            ShopPointService shopPointService) {
        Map<Integer, CityMenuAction> actions = new HashMap<>();
        for (CityMenuItemDefinition definition : definitions()) {
            actions.put(definition.slot(), player -> {
                PlotContext context = PlotContext.from(player, plotRepository, plotMemberRepository, shopPointService);
                if (!definition.isAvailable(player, context)) {
                    return;
                }
                definition.action().execute(player);
            });
        }
        return actions;
    }

    public static final class CityMenuHolder implements InventoryHolder {
        private final Map<Integer, CityMenuAction> actions;

        private CityMenuHolder(Map<Integer, CityMenuAction> actions) {
            this.actions = actions;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

        private void handleClick(Player player, int slot) {
            CityMenuAction action = actions.get(slot);
            if (action != null) {
                action.execute(player);
            }
        }
    }

    record PlotContext(boolean hasPlot,
                               boolean insidePlot,
                               boolean canManageMembers,
                               boolean ownerOrAdmin,
                               boolean nearOwnedShopPoint,
                               boolean nearShopPoint) {
        static PlotContext from(Player player,
                                PlotRepository plotRepository,
                                PlotMemberRepository plotMemberRepository,
                                ShopPointService shopPointService) {
            Optional<Plot> currentPlot = plotRepository.findContaining(player.getLocation());
            Optional<Plot> ownedPlot = plotRepository.findByOwner(player.getUniqueId()).stream().findFirst();
            Plot plot = ownedPlot.orElse(currentPlot.orElse(null));
            boolean hasPlot = plot != null;
            boolean insidePlot = currentPlot.isPresent();
            boolean ownerOrAdmin = hasPlot && (plot.isOwnedByPlayer(player.getUniqueId())
                    || player.hasPermission("realite.city.admin"));
            boolean canManageMembers = hasPlot && plot.ownerType() == PlotOwnerType.PLAYER
                    && (player.getUniqueId().equals(plot.ownerId())
                    || player.hasPermission("realite.city.admin"));
            boolean nearShopPoint = shopPointService.findNearest(player.getLocation(), 2.0).isPresent();
            boolean nearOwnedShopPoint = false;
            if (nearShopPoint) {
                nearOwnedShopPoint = shopPointService.findNearest(player.getLocation(), 2.0)
                        .flatMap(point -> plotRepository.findById(point.plotId()))
                        .filter(shopPlot -> shopPlot.type() == PlotType.SHOP)
                        .filter(shopPlot -> shopPlot.isOwnedByPlayer(player.getUniqueId())
                                || player.hasPermission("realite.city.admin")
                                || plotMemberRepository.findRole(shopPlot.id(), player.getUniqueId())
                                .map(role -> role == ru.realite.city.model.PlotMemberRole.TRUSTED)
                                .orElse(false))
                        .isPresent();
            }
            return new PlotContext(hasPlot, insidePlot, canManageMembers, ownerOrAdmin, nearOwnedShopPoint, nearShopPoint);
        }

        boolean hasPlot() {
            return hasPlot;
        }

        boolean isInsidePlot() {
            return insidePlot;
        }

        boolean canManageMembers() {
            return canManageMembers;
        }

        boolean isOwnerOrAdmin() {
            return ownerOrAdmin;
        }

        boolean isNearOwnedShopPoint() {
            return nearOwnedShopPoint;
        }

        boolean isNearShopPoint() {
            return nearShopPoint;
        }
    }
}
