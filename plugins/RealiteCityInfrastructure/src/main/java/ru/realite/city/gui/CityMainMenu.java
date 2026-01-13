package ru.realite.city.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.CityArea;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotMemberRole;
import ru.realite.city.model.PlotOwnerType;
import ru.realite.city.model.PlotType;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CityMainMenu {

        private static final int SIZE = 54;
        private static final int DETAILS_SIZE = 27;
        private static final int PLOTS_PER_PAGE = 45;
        private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

        private static CityMessages messages;
        private static CityConfig config;
        private static PlotRepository plotRepository;
        private static PlotMemberRepository plotMemberRepository;
        private static CityAreaRepository cityAreaRepository;

        private CityMainMenu() {
        }

        public static void init(
                        CityMessages messages,
                        CityConfig config,
                        PlotRepository plotRepository,
                        PlotMemberRepository plotMemberRepository,
                        CityAreaRepository cityAreaRepository) {
                CityMainMenu.messages = messages;
                CityMainMenu.config = config;
                CityMainMenu.plotRepository = plotRepository;
                CityMainMenu.plotMemberRepository = plotMemberRepository;
                CityMainMenu.cityAreaRepository = cityAreaRepository;
        }

        public static void open(Player player) {
                if (!isReady()) {
                        return;
                }
                openHub(player);
        }

        public static void openPlotsList(Player player, PlotFilter filter, int page) {
                if (!isReady() || player == null) {
                        return;
                }
                PlotFilter resolvedFilter = filter == null ? PlotFilter.ALL : filter;
                List<Plot> plots = new ArrayList<>(plotRepository.findAll());
                plots.sort(Comparator.comparingInt(Plot::number));
                plots = plots.stream().filter(resolvedFilter::matches).toList();

                int maxPage = Math.max(0, (plots.size() - 1) / PLOTS_PER_PAGE);
                int safePage = Math.max(0, Math.min(page, maxPage));

                Map<Integer, CityMenuAction> actions = new HashMap<>();
                CityMenuHolder holder = new CityMenuHolder(actions);

                Component title = messages.get("city.gui.plots.title", "Plots");
                Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
                fillBackground(inventory);

                if (plots.isEmpty()) {
                        inventory.setItem(22, simpleItem(
                                        Material.BARRIER,
                                        messages.get("city.gui.plots.empty", "No plots"),
                                        List.of()));
                } else {
                        int startIndex = safePage * PLOTS_PER_PAGE;
                        int endIndex = Math.min(plots.size(), startIndex + PLOTS_PER_PAGE);
                        int slot = 0;
                        for (int i = startIndex; i < endIndex; i++) {
                                Plot plot = plots.get(i);
                                ItemStack item = plotListItem(plot);
                                inventory.setItem(slot, item);
                                int clickSlot = slot;
                                actions.put(clickSlot, p -> openPlotDetails(p, plot.id(), resolvedFilter, safePage));
                                slot++;
                        }
                }

                renderPlotsFooter(inventory, actions, resolvedFilter, safePage, maxPage);

                player.openInventory(inventory);
        }

        public static void openPlotDetails(Player player, String plotId) {
                openPlotDetails(player, plotId, PlotFilter.ALL, 0);
        }

        public static void openPlotDetails(Player player, String plotId, PlotFilter filter, int page) {
                if (!isReady() || player == null) {
                        return;
                }
                Optional<Plot> plotOptional = plotRepository.findById(plotId);
                if (plotOptional.isEmpty()) {
                        messages.send(player, "city.plot.not-found", "", Map.of("id", String.valueOf(plotId)));
                        openPlotsList(player, filter, page);
                        return;
                }
                Plot plot = plotOptional.get();

                Map<Integer, CityMenuAction> actions = new HashMap<>();
                CityMenuHolder holder = new CityMenuHolder(actions);

                Component title = formatComponent("city.gui.plot.title", "Plot #" + plot.number(),
                                Map.of("number", String.valueOf(plot.number())));
                Inventory inventory = Bukkit.createInventory(holder, DETAILS_SIZE, title);
                fillBackground(inventory);

                inventory.setItem(13, plotDetailsItem(plot));

                boolean isAvailable = plot.ownerId() == null;
                if (isAvailable) {
                        inventory.setItem(10, actionItem(Material.EMERALD,
                                        "city.gui.plot.action.buy",
                                        "Buy plot",
                                        "city.gui.plot.action_lore.buy",
                                        "Buy this plot"));
                        actions.put(10, p -> p.performCommand("city plot buy " + plot.id()));
                }

                if (isShopRentAvailable(player, plot)) {
                        inventory.setItem(11, actionItem(Material.GOLD_INGOT,
                                        "city.gui.plot.action.rent",
                                        "Pay rent",
                                        "city.gui.plot.action_lore.rent",
                                        "Pay shop rent"));
                        actions.put(11, p -> p.performCommand("city shop rent pay " + plot.id()));
                }

                inventory.setItem(15, actionItem(Material.BOOK,
                                "city.gui.plot.action.owner",
                                "Owner",
                                "city.gui.plot.action_lore.owner",
                                "View owner"));
                actions.put(15, p -> p.performCommand("city plot info " + plot.id()));

                if (player.hasPermission("realite.city.admin")) {
                        inventory.setItem(16, actionItem(Material.ENDER_PEARL,
                                        "city.gui.plot.action.teleport",
                                        "Teleport",
                                        "city.gui.plot.action_lore.teleport",
                                        "Teleport to plot"));
                        actions.put(16, p -> p.performCommand("city plot goto " + plot.id()));
                }

                if (canManagePlot(player, plot)) {
                        inventory.setItem(22, actionItem(Material.PLAYER_HEAD,
                                        "city.gui.plot.action.members",
                                        "Members",
                                        "city.gui.plot.action_lore.members",
                                        "View members"));
                        actions.put(22, p -> p.performCommand("city plot members " + plot.id()));
                }

                if (isOwnerOrAdmin(player, plot)) {
                        inventory.setItem(21, actionItem(Material.BARRIER,
                                        "city.gui.plot.action.release",
                                        "Release",
                                        "city.gui.plot.action_lore.release",
                                        "Release plot"));
                        actions.put(21, p -> p.performCommand("city plot release " + plot.id()));
                }

                inventory.setItem(26, actionItem(Material.ARROW,
                                "city.gui.plot.action.back",
                                "Back",
                                null,
                                null));
                actions.put(26, p -> openPlotsList(p, filter, page));

                player.openInventory(inventory);
        }

        private static void openHub(Player player) {
                if (!isReady() || player == null) {
                        return;
                }
                Map<Integer, CityMenuAction> actions = new HashMap<>();
                CityMenuHolder holder = new CityMenuHolder(actions);

                Component title = messages.get("city.gui.hub.title", "City");
                Inventory inventory = Bukkit.createInventory(holder, SIZE, title);
                fillBackground(inventory);
                inventory.setItem(4, statusItem(player));

                HubContext ctx = HubContext.from(player, plotRepository, plotMemberRepository, cityAreaRepository);

                for (CityMenuItemDefinition def : definitions()) {
                        boolean available = def.isAvailable(player, ctx);
                        List<Component> lore = buildLore(def, available, player, ctx);
                        Component name = messages.get("city.gui.hub.item." + def.id() + ".name", def.id());
                        ItemStack item = CityMenuItemFactory.create(def.material(), name, lore, available);
                        inventory.setItem(def.slot(), item);
                        actions.put(def.slot(), p -> {
                                if (def.isAvailable(p, ctx)) {
                                        def.action().execute(p);
                                }
                        });
                }

                player.openInventory(inventory);
        }

        private static boolean isReady() {
                return messages != null
                                && config != null
                                && plotRepository != null
                                && plotMemberRepository != null
                                && cityAreaRepository != null;
        }

        private static List<Component> buildLore(
                        CityMenuItemDefinition def,
                        boolean available,
                        Player player,
                        HubContext ctx) {
                List<Component> lore = new ArrayList<>();

                String k1 = "city.gui.hub.item." + def.id() + ".lore1";
                String k2 = "city.gui.hub.item." + def.id() + ".lore2";

                if (!messages.getRaw(k1, "").isBlank()) {
                        lore.add(messages.get(k1, ""));
                }
                if (!messages.getRaw(k2, "").isBlank()) {
                        lore.add(messages.get(k2, ""));
                }

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

        private static String resolveUnavailableKey(CityMenuItemDefinition def, Player player, HubContext ctx) {
                if (def.permission() != null && player != null && !player.hasPermission(def.permission())) {
                        return "city.gui.unavailable.no-permission";
                }
                if (def.condition() != null && !def.condition().test(ctx)) {
                        if (def.unavailableReasonKey() != null) {
                                return def.unavailableReasonKey().apply(messages);
                        }
                }
                return "city.gui.unavailable.condition";
        }

        private static void fillBackground(Inventory inventory) {
                for (int i = 0; i < inventory.getSize(); i++) {
                        inventory.setItem(i, FILLER);
                }
        }

        /* ---------------- definitions ---------------- */

        private static List<CityMenuItemDefinition> definitions() {
                List<CityMenuItemDefinition> items = new ArrayList<>();

                items.add(new CityMenuItemDefinition(
                                "plots", 20, Material.MAP,
                                null, null, null,
                                p -> openPlotsList(p, PlotFilter.ALL, 0)));

                items.add(new CityMenuItemDefinition(
                                "residency", 22, Material.BOOK,
                                null, HubContext::questsAvailable,
                                m -> "city.gui.hub.unavailable.no-quests",
                                p -> p.performCommand("quest")));

                items.add(new CityMenuItemDefinition(
                                "market", 24, Material.CHEST,
                                null, null, null,
                                p -> p.performCommand("city market")));

                items.add(new CityMenuItemDefinition(
                                "info", 31, Material.NAME_TAG,
                                null, null, null,
                                p -> p.performCommand("city")));

                return items;
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

        record HubContext(boolean isResident, Optional<String> cityId, boolean questsAvailable) {
                static HubContext from(
                                Player player,
                                PlotRepository plotRepository,
                                PlotMemberRepository plotMemberRepository,
                                CityAreaRepository cityAreaRepository) {

                        if (player == null) {
                                return new HubContext(false, Optional.empty(), false);
                        }

                        boolean resident = CityMainMenu.isResident(player, plotRepository, plotMemberRepository);

                        Optional<String> cityId = Optional.empty();
                        if (cityAreaRepository != null) {
                                cityId = cityAreaRepository.findContaining(player.getLocation()).map(CityArea::id);
                        }

                        boolean questsAvailable = isQuestsAvailable();
                        return new HubContext(resident, cityId, questsAvailable);
                }
        }

        private enum PlotFilter {
                ALL,
                AVAILABLE,
                OCCUPIED;

                boolean matches(Plot plot) {
                        if (plot == null) {
                                return false;
                        }
                        return switch (this) {
                                case ALL -> true;
                                case AVAILABLE -> plot.ownerId() == null;
                                case OCCUPIED -> plot.ownerId() != null;
                        };
                }
        }

        private static boolean isResident(Player player,
                        PlotRepository plotRepository,
                        PlotMemberRepository plotMemberRepository) {
                if (player == null) {
                        return false;
                }
                if (!plotRepository.findByOwner(player.getUniqueId()).isEmpty()) {
                        return true;
                }
                for (Plot plot : plotRepository.findAll()) {
                        if (plotMemberRepository.isMember(plot.id(), player.getUniqueId())) {
                                return true;
                        }
                }
                return false;
        }

        private static boolean isQuestsAvailable() {
                var plugin = Bukkit.getPluginManager().getPlugin("RealiteQuests");
                return plugin != null && plugin.isEnabled();
        }

        private static ItemStack statusItem(Player player) {
                HubContext ctx = HubContext.from(player, plotRepository, plotMemberRepository, cityAreaRepository);
                String residencyKey = ctx.isResident()
                                ? "city.gui.hub.status.resident"
                                : "city.gui.hub.status.non_resident";
                Component residency = messages.get(residencyKey, ctx.isResident() ? "Resident" : "Not resident");

                String cityValue = ctx.cityId().orElse(messages.getRaw("city.gui.hub.status.city_none", "none"));
                Component cityLine = formatComponent("city.gui.hub.status.city", "City: {city}",
                                Map.of("city", cityValue));

                List<Component> lore = List.of(residency, cityLine);

                return CityMenuItemFactory.create(
                                Material.NAME_TAG,
                                messages.get("city.gui.hub.status.name", "Status"),
                                lore,
                                true);
        }

        private static void renderPlotsFooter(Inventory inventory,
                        Map<Integer, CityMenuAction> actions,
                        PlotFilter filter,
                        int page,
                        int maxPage) {
                inventory.setItem(45, filterItem(filter, PlotFilter.ALL));
                actions.put(45, p -> openPlotsList(p, PlotFilter.ALL, 0));

                inventory.setItem(46, filterItem(filter, PlotFilter.AVAILABLE));
                actions.put(46, p -> openPlotsList(p, PlotFilter.AVAILABLE, 0));

                inventory.setItem(47, filterItem(filter, PlotFilter.OCCUPIED));
                actions.put(47, p -> openPlotsList(p, PlotFilter.OCCUPIED, 0));

                inventory.setItem(49, pageIndicatorItem(page, maxPage));

                inventory.setItem(51, actionItem(Material.ARROW,
                                "city.gui.plots.prev",
                                "<",
                                null,
                                null));
                actions.put(51, p -> openPlotsList(p, filter, Math.max(0, page - 1)));

                inventory.setItem(53, actionItem(Material.ARROW,
                                "city.gui.plots.next",
                                ">",
                                null,
                                null));
                actions.put(53, p -> openPlotsList(p, filter, page + 1));

                inventory.setItem(52, actionItem(Material.BARRIER,
                                "city.gui.plots.back",
                                "Back",
                                null,
                                null));
                actions.put(52, CityMainMenu::openHub);
        }

        private static ItemStack filterItem(PlotFilter current, PlotFilter itemFilter) {
                boolean selected = current == itemFilter;
                Material material = switch (itemFilter) {
                        case ALL -> selected ? Material.WRITTEN_BOOK : Material.BOOK;
                        case AVAILABLE -> selected ? Material.LIME_STAINED_GLASS_PANE : Material.LIME_DYE;
                        case OCCUPIED -> selected ? Material.RED_STAINED_GLASS_PANE : Material.RED_DYE;
                };
                String key = switch (itemFilter) {
                        case ALL -> "city.gui.plots.filter.all";
                        case AVAILABLE -> "city.gui.plots.filter.available";
                        case OCCUPIED -> "city.gui.plots.filter.occupied";
                };
                List<Component> lore = new ArrayList<>();
                if (selected) {
                        lore.add(messages.get("city.gui.plots.filter.selected", "Selected"));
                }
                return CityMenuItemFactory.create(material, messages.get(key, itemFilter.name()), lore, true);
        }

        private static ItemStack plotListItem(Plot plot) {
                String owner = formatOwner(plot);
                String statusKey = plot.ownerId() == null
                                ? "city.gui.plots.status.available"
                                : "city.gui.plots.status.occupied";
                Component status = messages.get(statusKey, plot.ownerId() == null ? "Available" : "Occupied");
                Component name = formatComponent("city.gui.plots.item.name", "#{number} ({id})",
                                Map.of("number", String.valueOf(plot.number()), "id", plot.id()));
                List<Component> lore = List.of(
                                formatComponent("city.gui.plots.item.lore1", "Type: {type}",
                                                Map.of("type", plot.type() == null ? "?" : plot.type().displayName())),
                                formatComponent("city.gui.plots.item.lore2", "Price: {price}",
                                                Map.of("price", String.valueOf(plot.price()))),
                                formatComponent("city.gui.plots.item.lore3", "Owner: {owner}",
                                                Map.of("owner", owner)),
                                status);
                return CityMenuItemFactory.create(plotMaterial(plot), name, lore, true);
        }

        private static ItemStack plotDetailsItem(Plot plot) {
                String owner = formatOwner(plot);
                List<Component> lore = List.of(
                                formatComponent("city.gui.plot.info.lore1", "ID: {id}",
                                                Map.of("id", plot.id())),
                                formatComponent("city.gui.plot.info.lore2", "Type: {type}",
                                                Map.of("type", plot.type() == null ? "?" : plot.type().displayName())),
                                formatComponent("city.gui.plot.info.lore3", "Price: {price}",
                                                Map.of("price", String.valueOf(plot.price()))),
                                formatComponent("city.gui.plot.info.lore4", "Owner: {owner}",
                                                Map.of("owner", owner)),
                                formatComponent("city.gui.plot.info.lore5", "World: {world}",
                                                Map.of("world", plot.world())));
                return CityMenuItemFactory.create(
                                Material.OAK_SIGN,
                                messages.get("city.gui.plot.info.name", "Plot info"),
                                lore,
                                true);
        }

        private static ItemStack actionItem(Material material,
                        String nameKey,
                        String fallbackName,
                        String loreKey,
                        String loreFallback) {
                Component name = messages.get(nameKey, fallbackName);
                List<Component> lore = new ArrayList<>();
                if (loreKey != null) {
                        String raw = messages.getRaw(loreKey, loreFallback);
                        if (raw != null && !raw.isBlank()) {
                                lore.add(LEGACY.deserialize(raw));
                        }
                }
                return CityMenuItemFactory.create(material, name, lore, true);
        }

        private static ItemStack pageIndicatorItem(int page, int maxPage) {
                Component name = formatComponent(
                                "city.gui.plots.page",
                                "Page {current}/{total}",
                                Map.of(
                                                "current", String.valueOf(page + 1),
                                                "total", String.valueOf(maxPage + 1)));
                return CityMenuItemFactory.create(Material.PAPER, name, List.of(), true);
        }

        private static ItemStack simpleItem(Material material, Component name, List<Component> lore) {
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
                        if (lore != null && !lore.isEmpty()) {
                                meta.lore(lore);
                        }
                        item.setItemMeta(meta);
                }
                return item;
        }

        private static Component formatComponent(String key, String fallback, Map<String, String> vars) {
                String raw = messages.getRaw(key, fallback);
                if (raw == null) {
                        raw = "";
                }
                if (vars != null) {
                        for (var entry : vars.entrySet()) {
                                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
                        }
                }
                return LEGACY.deserialize(raw);
        }

        private static Material plotMaterial(Plot plot) {
                if (plot == null || plot.type() == null) {
                        return Material.PAPER;
                }
                return switch (plot.type()) {
                        case HOME -> Material.GRASS_BLOCK;
                        case SHOP -> Material.CHEST;
                };
        }

        private static String formatOwner(Plot plot) {
                if (plot == null || plot.ownerId() == null || plot.ownerType() == null) {
                        return messages.getRaw("city.plot.owner.none", "none");
                }
                if (plot.ownerType() == PlotOwnerType.PLAYER) {
                        OfflinePlayer owner = Bukkit.getOfflinePlayer(plot.ownerId());
                        return owner.getName() == null ? plot.ownerId().toString() : owner.getName();
                }
                return "guild:" + plot.ownerId();
        }

        private static boolean isShopRentAvailable(Player player, Plot plot) {
                if (player == null || plot == null) {
                        return false;
                }
                if (!config.shopRentEnabled()) {
                        return false;
                }
                if (plot.type() != PlotType.SHOP || plot.ownerId() == null) {
                        return false;
                }
                return isOwnerTrustedOrAdmin(player, plot);
        }

        private static boolean isOwnerTrustedOrAdmin(Player player, Plot plot) {
                if (player.hasPermission("realite.city.admin")) {
                        return true;
                }
                if (plot.ownerType() != PlotOwnerType.PLAYER) {
                        return false;
                }
                if (plot.isOwnedByPlayer(player.getUniqueId())) {
                        return true;
                }
                return plotMemberRepository
                                .findRole(plot.id(), player.getUniqueId())
                                .map(role -> role == PlotMemberRole.TRUSTED)
                                .orElse(false);
        }

        private static boolean canManagePlot(Player player, Plot plot) {
                return isOwnerOrAdmin(player, plot)
                                && plot.ownerType() == PlotOwnerType.PLAYER;
        }

        private static boolean isOwnerOrAdmin(Player player, Plot plot) {
                return player != null
                                && plot != null
                                && (player.hasPermission("realite.city.admin")
                                                || plot.isOwnedByPlayer(player.getUniqueId()));
        }
}
