package ru.realite.city.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotOwnerType;
import ru.realite.city.model.PlotType;
import ru.realite.city.service.ShopPointService;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.*;

public final class CityMainMenu {

        private static final int SIZE = 54;
        private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        private static CityMessages messages;
        private static PlotRepository plotRepository;
        private static PlotMemberRepository plotMemberRepository;
        private static ShopPointService shopPointService;

        private CityMainMenu() {
        }

        public static void init(
                        CityMessages messages,
                        PlotRepository plotRepository,
                        PlotMemberRepository plotMemberRepository,
                        ShopPointService shopPointService) {
                CityMainMenu.messages = messages;
                CityMainMenu.plotRepository = plotRepository;
                CityMainMenu.plotMemberRepository = plotMemberRepository;
                CityMainMenu.shopPointService = shopPointService;
        }

        public static void open(Player player) {
                if (messages == null || plotRepository == null || plotMemberRepository == null
                                || shopPointService == null) {
                        return;
                }

                CityMenuHolder holder = new CityMenuHolder(buildActions(
                                plotRepository,
                                plotMemberRepository,
                                shopPointService));

                Component title = messages.get("city.gui.title", "City");
                Inventory inventory = Bukkit.createInventory(holder, SIZE, title);

                fillBackground(inventory);
                renderItems(player, inventory);

                player.openInventory(inventory);
        }

        private static void renderItems(Player player, Inventory inventory) {
                PlotContext ctx = PlotContext.from(player, plotRepository, plotMemberRepository, shopPointService);

                for (CityMenuItemDefinition def : definitions()) {
                        boolean available = def.isAvailable(player, ctx);
                        List<Component> lore = buildLore(def, available, player, ctx);
                        Component name = messages.get("city.gui.item." + def.id() + ".name", def.id());
                        ItemStack item = CityMenuItemFactory.create(def.material(), name, lore, available);
                        inventory.setItem(def.slot(), item);
                }
        }

        private static List<Component> buildLore(
                        CityMenuItemDefinition def,
                        boolean available,
                        Player player,
                        PlotContext ctx) {
                List<Component> lore = new ArrayList<>();

                String k1 = "city.gui.item." + def.id() + ".lore1";
                String k2 = "city.gui.item." + def.id() + ".lore2";

                if (!messages.getRaw(k1, "").isBlank())
                        lore.add(messages.get(k1, ""));
                if (!messages.getRaw(k2, "").isBlank())
                        lore.add(messages.get(k2, ""));

                if (available) {
                        lore.add(messages.get("city.gui.available", ""));
                } else {
                        lore.add(
                                        CityMenuItemFactory.unavailableReason(
                                                        messages,
                                                        resolveUnavailableKey(def, player, ctx),
                                                        messages.getRaw("city.gui.unavailable.condition", "")));
                }
                return lore;
        }

        private static String resolveUnavailableKey(
                        CityMenuItemDefinition def,
                        Player player,
                        PlotContext ctx) {
                if (def.permission() != null && !player.hasPermission(def.permission())) {
                        return "city.gui.unavailable.no-permission";
                }
                if (def.condition() != null && !def.condition().test(ctx)) {
                        if (def.unavailableReasonKey() != null) {
                                return def.unavailableReasonKey().apply(messages);
                        }
                        if (!ctx.hasPlot()) {
                                return "city.gui.unavailable.no-plot";
                        }
                }
                return "city.gui.unavailable.condition";
        }

        private static void fillBackground(Inventory inventory) {
                for (int i = 0; i < inventory.getSize(); i++) {
                        inventory.setItem(i, FILLER);
                }

                Component title = messages.get("city.gui.title", "City")
                                .color(NamedTextColor.GOLD)
                                .decoration(TextDecoration.ITALIC, false);

                Component hint = messages.get("city.gui.hint", "")
                                .color(NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false);

                inventory.setItem(
                                4,
                                CityMenuItemFactory.create(
                                                Material.NAME_TAG,
                                                title,
                                                List.of(hint),
                                                true));
        }

        /* ---------------- definitions ---------------- */

        private static List<CityMenuItemDefinition> definitions() {
                List<CityMenuItemDefinition> items = new ArrayList<>();

                items.add(new CityMenuItemDefinition(
                                "help", 10, Material.BOOK,
                                null, null, null,
                                p -> p.performCommand("city")));

                items.add(new CityMenuItemDefinition(
                                "plot-nearby", 19, Material.MAP,
                                null, null, null,
                                p -> p.performCommand("city plot nearby")));

                items.add(new CityMenuItemDefinition(
                                "plot-info", 20, Material.PAPER,
                                null, PlotContext::hasPlot,
                                m -> "city.gui.unavailable.no-plot",
                                p -> p.performCommand("city plot info")));

                items.add(new CityMenuItemDefinition(
                                "plot-buy", 21, Material.EMERALD,
                                null, PlotContext::insidePlot,
                                m -> "city.gui.unavailable.no-plot",
                                p -> p.performCommand("city plot buy")));

                items.add(new CityMenuItemDefinition(
                                "plot-members", 23, Material.PLAYER_HEAD,
                                null, PlotContext::canManageMembers,
                                m -> "city.gui.unavailable.no-plot",
                                p -> p.performCommand("city plot members")));

                items.add(new CityMenuItemDefinition(
                                "plot-release", 28, Material.BARRIER,
                                null, PlotContext::ownerOrAdmin,
                                m -> "city.gui.unavailable.no-plot",
                                p -> p.performCommand("city plot release")));

                items.add(new CityMenuItemDefinition(
                                "market-list", 31, Material.CHEST,
                                null, null, null,
                                p -> p.performCommand("city market")));

                items.add(new CityMenuItemDefinition(
                                "market-goto", 36, Material.ENDER_PEARL,
                                "realite.city.market.tp", null, null,
                                p -> p.performCommand("city market goto")));

                items.add(new CityMenuItemDefinition(
                                "shop-setup", 40, Material.ANVIL,
                                null, PlotContext::nearOwnedShopPoint,
                                m -> "city.gui.unavailable.no-plot",
                                p -> p.performCommand("city shop setup")));

                items.add(new CityMenuItemDefinition(
                                "area-list", 13, Material.PAPER,
                                "realite.city.admin", null, null,
                                p -> p.performCommand("city area list")));

                return items;
        }

        /* ---------------- actions ---------------- */

        private static Map<Integer, CityMenuAction> buildActions(
                        PlotRepository plotRepository,
                        PlotMemberRepository plotMemberRepository,
                        ShopPointService shopPointService) {
                Map<Integer, CityMenuAction> actions = new HashMap<>();

                for (CityMenuItemDefinition def : definitions()) {
                        actions.put(def.slot(), player -> {
                                PlotContext ctx = PlotContext.from(
                                                player,
                                                plotRepository,
                                                plotMemberRepository,
                                                shopPointService);
                                if (def.isAvailable(player, ctx)) {
                                        def.action().execute(player);
                                }
                        });
                }
                return actions;
        }

        /* ---------------- holder ---------------- */

        public static final class CityMenuHolder implements InventoryHolder {
                private final Map<Integer, CityMenuAction> actions;

                private CityMenuHolder(Map<Integer, CityMenuAction> actions) {
                        this.actions = actions;
                }

                @Override
                public Inventory getInventory() {
                        return null;
                }

                void handleClick(Player player, int slot) {
                        CityMenuAction action = actions.get(slot);
                        if (action != null) {
                                action.execute(player);
                        }
                }
        }

        /* ---------------- context ---------------- */

        record PlotContext(
                        boolean hasPlot,
                        boolean insidePlot,
                        boolean canManageMembers,
                        boolean ownerOrAdmin,
                        boolean nearOwnedShopPoint,
                        boolean nearShopPoint) {
                static PlotContext from(
                                Player player,
                                PlotRepository plotRepository,
                                PlotMemberRepository plotMemberRepository,
                                ShopPointService shopPointService) {
                        Optional<Plot> current = plotRepository.findContaining(player.getLocation());
                        Optional<Plot> owned = plotRepository.findByOwner(player.getUniqueId()).stream().findFirst();

                        Optional<Plot> plotOpt = owned.isPresent() ? owned : current;

                        boolean hasPlot = plotOpt.isPresent();
                        boolean insidePlot = current.isPresent();

                        boolean ownerOrAdmin = plotOpt
                                        .map(p -> p.isOwnedByPlayer(player.getUniqueId()))
                                        .orElse(false)
                                        || player.hasPermission("realite.city.admin");

                        boolean canManageMembers = plotOpt
                                        .filter(p -> p.ownerType() == PlotOwnerType.PLAYER)
                                        .map(p -> player.getUniqueId().equals(p.ownerId()))
                                        .orElse(false)
                                        || player.hasPermission("realite.city.admin");

                        boolean nearShop = shopPointService.findNearest(player.getLocation(), 2.0).isPresent();

                        boolean nearOwnedShop = shopPointService
                                        .findNearest(player.getLocation(), 2.0)
                                        .flatMap(point -> plotRepository.findById(point.plotId()))
                                        .filter(p -> p.type() == PlotType.SHOP)
                                        .filter(p -> p.isOwnedByPlayer(player.getUniqueId())
                                                        || player.hasPermission("realite.city.admin")
                                                        || plotMemberRepository.findRole(p.id(), player.getUniqueId())
                                                                        .map(r -> r == ru.realite.city.model.PlotMemberRole.TRUSTED)
                                                                        .orElse(false))
                                        .isPresent();

                        return new PlotContext(
                                        hasPlot,
                                        insidePlot,
                                        canManageMembers,
                                        ownerOrAdmin,
                                        nearOwnedShop,
                                        nearShop);
                }
        }
}