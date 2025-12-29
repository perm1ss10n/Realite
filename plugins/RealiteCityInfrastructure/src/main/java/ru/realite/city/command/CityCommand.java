package ru.realite.city.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.CityArea;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotMemberRole;
import ru.realite.city.model.PlotType;
import ru.realite.city.model.ShopPoint;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.service.CityAreaSelectionService.Selection;
import ru.realite.city.service.EconomyService;
import ru.realite.city.service.PlotCleanupService;
import ru.realite.city.service.PlotService;
import ru.realite.city.service.ShopPointService;
import ru.realite.city.service.ShopRentService;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CityCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "realite.city.admin";

    private final CityAreaRepository cityAreaRepository;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;
    private final PlotService plotService;
    private final PlotCleanupService plotCleanupService;
    private final CityAreaSelectionService selectionService;
    private final CityMessages messages;
    private final CityConfig config;
    private final EconomyService economyService;
    private final ShopPointService shopPointService;
    private final ShopRentService shopRentService;

    public CityCommand(
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            PlotService plotService,
            PlotCleanupService plotCleanupService,
            CityAreaSelectionService selectionService,
            CityMessages messages,
            CityConfig config,
            EconomyService economyService,
            ShopPointService shopPointService,
            ShopRentService shopRentService) {
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.plotService = plotService;
        this.plotCleanupService = plotCleanupService;
        this.selectionService = selectionService;
        this.messages = messages;
        this.config = config;
        this.economyService = economyService;
        this.shopPointService = shopPointService;
        this.shopRentService = shopRentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }
        String root = args[0].toLowerCase();
        switch (root) {
            case "area" -> handleArea(sender, args);
            case "plot" -> handlePlot(sender, args);
            case "shop" -> handleShop(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleArea(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "city.no-permission", "");
            return;
        }
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "wand" -> {
                selectionService.enableWand(player.getUniqueId());
                messages.send(player, "city.area.wand-enabled", "");
            }
            case "create" -> handleCreateArea(player, args);
            case "delete" -> handleDeleteArea(sender, args);
            case "list" -> handleListAreas(sender);
            default -> sendUsage(sender);
        }
    }

    private void handlePlot(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "list" -> handleListPlots(sender);
            case "info" -> handlePlotInfo(sender, args);
            case "buy" -> handlePlotBuy(sender, args);
            case "nearby" -> handlePlotNearby(sender, args);
            case "create" -> handlePlotCreate(sender, args);
            case "delete" -> handlePlotDelete(sender, args);
            case "addmember" -> handlePlotAddMember(sender, args, PlotMemberRole.MEMBER);
            case "trust" -> handlePlotAddMember(sender, args, PlotMemberRole.TRUSTED);
            case "removemember" -> handlePlotRemoveMember(sender, args);
            case "untrust" -> handlePlotRemoveMember(sender, args);
            case "members" -> handlePlotMembers(sender, args);
            case "transfer" -> handlePlotTransfer(sender, args);
            case "sell" -> handlePlotSell(sender, args);
            case "accept" -> handlePlotAccept(sender, args);
            case "release" -> handlePlotRelease(sender, args);
            case "goto" -> handlePlotGoto(sender, args);
            default -> sendUsage(sender);
        }
    }

    private void handleShop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "set" -> handleShopSet(sender);
            case "remove" -> handleShopRemove(sender, args);
            case "list" -> handleShopList(sender, args);
            case "info" -> handleShopInfo(sender);
            case "rent" -> handleShopRent(sender, args);
            default -> sendUsage(sender);
        }
    }

    private void handleCreateArea(Player player, String[] args) {
        if (args.length < 3) {
            messages.send(player, "city.area.create.usage", "");
            return;
        }
        String id = args[2];
        Optional<CityArea> existing = cityAreaRepository.findById(id);
        if (existing.isPresent()) {
            messages.send(player, "city.area.exists", "", Map.of("id", id));
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            messages.send(player, "city.area.selection-missing", "");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "city.area.invalid-world", "");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(player, "city.area.mismatched-world", "");
            return;
        }
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        CityArea area = new CityArea(
                id,
                pos1.getWorld().getName(),
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                System.currentTimeMillis());
        cityAreaRepository.upsert(area);
        messages.send(player, "city.area.created", "", Map.of("id", id));
    }

    private void handleDeleteArea(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "city.area.delete.usage", "");
            return;
        }
        String id = args[2];
        if (cityAreaRepository.delete(id)) {
            messages.send(sender, "city.area.deleted", "", Map.of("id", id));
        } else {
            messages.send(sender, "city.area.not-found", "", Map.of("id", id));
        }
    }

    private void handleListAreas(CommandSender sender) {
        List<CityArea> areas = cityAreaRepository.findAll();
        if (areas.isEmpty()) {
            messages.send(sender, "city.area.none", "");
            return;
        }
        areas.sort(Comparator.comparing(CityArea::id));
        messages.send(sender, "city.area.list.header", "");
        for (CityArea area : areas) {
            messages.send(sender, "city.area.list.line", "",
                    Map.ofEntries(
                            Map.entry("id", area.id()),
                            Map.entry("world", area.world()),
                            Map.entry("minX", String.valueOf(area.minX())),
                            Map.entry("minY", String.valueOf(area.minY())),
                            Map.entry("minZ", String.valueOf(area.minZ())),
                            Map.entry("maxX", String.valueOf(area.maxX())),
                            Map.entry("maxY", String.valueOf(area.maxY())),
                            Map.entry("maxZ", String.valueOf(area.maxZ()))));
        }
    }

    private void handleListPlots(CommandSender sender) {
        List<Plot> plots = plotRepository.findAll();
        if (plots.isEmpty()) {
            messages.send(sender, "city.plot.list.none", "");
            return;
        }
        plots.sort(Comparator.comparingInt(Plot::number));
        messages.send(sender, "city.plot.list.header", "");
        for (Plot plot : plots) {
            messages.send(sender, "city.plot.list.line", "",
                    Map.ofEntries(
                            Map.entry("number", String.valueOf(plot.number())),
                            Map.entry("id", plot.id()),
                            Map.entry("type", plot.type().displayName()),
                            Map.entry("price", String.valueOf(plot.price())),
                            Map.entry("owner", formatOwner(plot.ownerUuid()))));
        }
    }

    private void handlePlotInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "city.plot.info.usage", "");
            return;
        }
        String reference = args[2];
        Optional<Plot> plotOptional = resolvePlotReference(reference);
        if (plotOptional.isEmpty()) {
            messages.send(sender, "city.plot.not-found", "", Map.of("id", reference));
            return;
        }
        Plot plot = plotOptional.get();
        messages.send(sender, "city.plot.info", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("number", String.valueOf(plot.number())),
                        Map.entry("type", plot.type().displayName()),
                        Map.entry("price", String.valueOf(plot.price())),
                        Map.entry("owner", formatOwner(plot.ownerUuid())),
                        Map.entry("world", plot.world()),
                        Map.entry("x1", String.valueOf(plot.x1())),
                        Map.entry("y1", String.valueOf(plot.y1())),
                        Map.entry("z1", String.valueOf(plot.z1())),
                        Map.entry("x2", String.valueOf(plot.x2())),
                        Map.entry("y2", String.valueOf(plot.y2())),
                        Map.entry("z2", String.valueOf(plot.z2()))));

    }

    private void handlePlotBuy(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Plot plot;
        if (args.length < 3) {
            Optional<Plot> plotOptional = plotService.findContaining(player.getLocation());
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.plot.buy.not-in-plot", "");
                return;
            }
            plot = plotOptional.get();
        } else {
            String reference = args[2];
            Optional<Plot> plotOptional = resolvePlotReference(reference);
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.plot.not-found", "", Map.of("id", reference));
                return;
            }
            plot = plotOptional.get();
        }
        String id = plot.id();
        PlotService.BuyResult result = plotService.buyPlot(player, id);
        switch (result) {
            case SUCCESS -> messages.send(player, "city.plot.buy.success", "",
                    Map.ofEntries(
                            Map.entry("id", id),
                            Map.entry("number", String.valueOf(plot.number()))));
            case NOT_FOUND -> messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            case ALREADY_OWNED -> messages.send(player, "city.plot.buy.already-owned", "",
                    Map.ofEntries(
                            Map.entry("id", id),
                            Map.entry("number", String.valueOf(plot.number()))));
            case TYPE_DISABLED -> messages.send(player, "city.plot.type-disabled", "",
                    Map.of("type",
                            plotRepository.findById(id).map(existing -> existing.type().displayName())
                                    .orElse(messages.getRaw("city.plot.type.unknown", ""))));
            case LIMIT_REACHED -> messages.send(player, "city.plot.limit-reached", "",
                    Map.of("limit", String.valueOf(config.limitFor(plot.type()))));
            case NOT_ENOUGH_MONEY -> messages.send(player, "city.plot.not-enough-money", "");
            case NO_ECONOMY -> messages.send(player, "city.plot.no-economy", "");
        }
    }

    private void handlePlotNearby(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        int radius = config.plotNearbyDefaultRadius();
        if (args.length >= 3) {
            try {
                radius = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                messages.send(player, "city.plot.nearby.usage", "");
                return;
            }
        }

        final int r = radius;

        Location location = player.getLocation();
        if (location.getWorld() == null) {
            messages.send(player, "city.plot.nearby.none", "");
            return;
        }
        List<PlotDistance> nearby = plotRepository.findAll().stream()
                .filter(plot -> plot.ownerUuid() == null)
                .filter(plot -> plot.world().equals(location.getWorld().getName()))
                .map(plot -> new PlotDistance(plot, distanceToPlot(location, plot)))
                .filter(distance -> distance.distance() <= r)
                .sorted(Comparator.comparingDouble(PlotDistance::distance))
                .limit(10)
                .toList();
        if (nearby.isEmpty()) {
            messages.send(player, "city.plot.nearby.none", "");
            return;
        }
        messages.send(player, "city.plot.nearby.header", "");
        for (PlotDistance distance : nearby) {
            Plot plot = distance.plot();
            messages.send(player, "city.plot.nearby.line", "",
                    Map.ofEntries(
                            Map.entry("number", String.valueOf(plot.number())),
                            Map.entry("id", plot.id()),
                            Map.entry("type", plot.type().displayName()),
                            Map.entry("price", String.valueOf(plot.price())),
                            Map.entry("dist", String.valueOf(Math.round(distance.distance())))));
        }
        messages.send(player, "city.plot.nearby.hint.buy", "");
        messages.send(player, "city.plot.nearby.hint.info", "");
    }

    private void handlePlotCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "city.no-permission", "");
            return;
        }
        if (args.length < 5) {
            messages.send(player, "city.plot.create.usage", "");
            return;
        }
        String id = args[2];
        PlotType type = PlotType.fromToken(args[3]);
        if (type == null) {
            messages.send(player, "city.plot.create.unknown-type", "");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            messages.send(player, "city.plot.create.invalid-price", "");
            return;
        }
        if (plotRepository.findById(id).isPresent()) {
            messages.send(player, "city.plot.create.exists", "", Map.of("id", id));
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            messages.send(player, "city.area.selection-missing", "");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "city.area.invalid-world", "");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(player, "city.area.mismatched-world", "");
            return;
        }
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int number = plotRepository.nextNumber();
        Plot plot = new Plot(
                id,
                number,
                type,
                world.getName(),
                minX,
                world.getMinHeight(),
                minZ,
                maxX,
                world.getMaxHeight(),
                maxZ,
                price,
                null,
                System.currentTimeMillis(),
                0L);
        plotRepository.upsert(plot);
        messages.send(player, "city.plot.created", "",
                Map.ofEntries(
                        Map.entry("id", id),
                        Map.entry("number", String.valueOf(number))));
    }

    private void handlePlotDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "city.no-permission", "");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "city.plot.delete.usage", "");
            return;
        }
        String id = args[2];
        if (plotRepository.delete(id)) {
            messages.send(sender, "city.plot.deleted", "", Map.of("id", id));
        } else {
            messages.send(sender, "city.plot.not-found", "", Map.of("id", id));
        }
    }

    private void handlePlotAddMember(CommandSender sender, String[] args, PlotMemberRole role) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.plot.member.add.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.member.self", "");
            return;
        }
        plotMemberRepository.upsert(id, targetId, role);
        messages.send(player, "city.plot.member.added", "",
                Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
    }

    private void handlePlotRemoveMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.plot.member.remove.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (plotMemberRepository.remove(id, targetId)) {
            messages.send(player, "city.plot.member.removed", "",
                    Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
        } else {
            messages.send(player, "city.plot.member.not-found", "");
        }
    }

    private void handlePlotMembers(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.plot.members.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        Map<UUID, PlotMemberRole> members = plotMemberRepository.findMembers(id);
        messages.send(player, "city.plot.members.header", "", Map.of("id", id));
        if (members.isEmpty()) {
            messages.send(player, "city.plot.members.empty", "");
            return;
        }
        for (Map.Entry<UUID, PlotMemberRole> entry : members.entrySet()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
            String name = member.getName() == null ? entry.getKey().toString() : member.getName();
            messages.send(player, "city.plot.members.line", "",
                    Map.ofEntries(
                            Map.entry("player", name),
                            Map.entry("role", entry.getValue().name())));
        }
    }

    private void handlePlotTransfer(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.plot.transfer.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.transfer.self", "");
            return;
        }
        long expiresAt = System.currentTimeMillis() + 120_000L;
        plotService.createTransferOffer(id, targetId, expiresAt);
        messages.send(player, "city.plot.transfer.sent", "",
                Map.ofEntries(
                        Map.entry("player", target.getName() == null ? targetId.toString() : target.getName()),
                        Map.entry("id", id)));
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                messages.send(online, "city.plot.transfer.sent", "",
                        Map.ofEntries(
                                Map.entry("player", player.getName()),
                                Map.entry("id", id)));
            }
        }
    }

    private void handlePlotSell(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 5) {
            messages.send(player, "city.plot.sell.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            messages.send(player, "city.plot.sell.invalid-price", "");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.sell.self", "");
            return;
        }
        long expiresAt = System.currentTimeMillis() + 120_000L;
        plotService.createSellOffer(id, targetId, expiresAt, price);
        messages.send(player, "city.plot.sell.sent", "",
                Map.ofEntries(
                        Map.entry("player", target.getName() == null ? targetId.toString() : target.getName()),
                        Map.entry("id", id),
                        Map.entry("price", String.valueOf(price))));
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                messages.send(online, "city.plot.sell.sent", "",
                        Map.ofEntries(
                                Map.entry("player", player.getName()),
                                Map.entry("id", id),
                                Map.entry("price", String.valueOf(price))));
            }
        }
    }

    private void handlePlotAccept(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.plot.accept.usage", "");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        Optional<PlotService.PendingSell> pendingSell = plotService.getPendingSell(id);
        if (pendingSell.isPresent()) {
            handleSellAccept(player, plot, pendingSell.get());
            return;
        }
        Optional<PlotService.PendingTransfer> pendingTransfer = plotService.getPendingTransfer(id);
        if (pendingTransfer.isPresent()) {
            handleTransferAccept(player, plot, pendingTransfer.get());
            return;
        }
        messages.send(player, "city.plot.transfer.expired", "",
                Map.of("id", id));
    }

    private void handleTransferAccept(Player player, Plot plot, PlotService.PendingTransfer pending) {
        if (!player.getUniqueId().equals(pending.targetUuid())) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        if (plotService.isLimitReached(player, plot.type())) {
            messages.send(player, "city.plot.transfer.limit-reached", "",
                    Map.of("limit", String.valueOf(config.limitFor(plot.type()))));
            return;
        }
        plotService.clearPendingOffers(plot.id());
        Plot updated = new Plot(
                plot.id(),
                plot.number(),
                plot.type(),
                plot.world(),
                plot.x1(),
                plot.y1(),
                plot.z1(),
                plot.x2(),
                plot.y2(),
                plot.z2(),
                plot.price(),
                player.getUniqueId(),
                plot.createdAt(),
                plot.rentPaidUntil());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        messages.send(player, "city.plot.transfer.accepted", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("number", String.valueOf(plot.number()))));
    }

    private void handleSellAccept(Player player, Plot plot, PlotService.PendingSell pending) {
        if (!player.getUniqueId().equals(pending.targetUuid())) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        if (plotService.isLimitReached(player, plot.type())) {
            messages.send(player, "city.plot.transfer.limit-reached", "",
                    Map.of("limit", String.valueOf(config.limitFor(plot.type()))));
            return;
        }
        if (pending.price() > 0) {
            if (economyService == null || !economyService.isAvailable()) {
                messages.send(player, "city.plot.sell.no-economy", "");
                return;
            }
            if (!economyService.has(player, pending.price())) {
                messages.send(player, "city.plot.not-enough-money", "");
                return;
            }
            if (!economyService.withdraw(player, pending.price())) {
                messages.send(player, "city.plot.not-enough-money", "");
                return;
            }
            if (plot.ownerUuid() != null) {
                economyService.deposit(Bukkit.getOfflinePlayer(plot.ownerUuid()), pending.price());
            }
        }
        plotService.clearPendingOffers(plot.id());
        Plot updated = new Plot(
                plot.id(),
                plot.number(),
                plot.type(),
                plot.world(),
                plot.x1(),
                plot.y1(),
                plot.z1(),
                plot.x2(),
                plot.y2(),
                plot.z2(),
                plot.price(),
                player.getUniqueId(),
                plot.createdAt(),
                plot.rentPaidUntil());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        messages.send(player, "city.plot.transfer.accepted", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("number", String.valueOf(plot.number()))));
    }

    private void handlePlotRelease(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Plot plot;
        if (args.length < 3) {
            Optional<Plot> plotOptional = plotService.findContaining(player.getLocation());
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.plot.buy.not-in-plot", "");
                return;
            }
            plot = plotOptional.get();
        } else {
            String reference = args[2];
            Optional<Plot> plotOptional = resolvePlotReference(reference);
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.plot.not-found", "", Map.of("id", reference));
                return;
            }
            plot = plotOptional.get();
        }
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "", Map.of("id", plot.id()));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "");
            return;
        }
        Plot updated = new Plot(
                plot.id(),
                plot.number(),
                plot.type(),
                plot.world(),
                plot.x1(),
                plot.y1(),
                plot.z1(),
                plot.x2(),
                plot.y2(),
                plot.z2(),
                plot.price(),
                null,
                plot.createdAt(),
                0L);
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        plotService.clearPendingOffers(plot.id());
        messages.send(player, "city.plot.release.success", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("number", String.valueOf(plot.number()))));
        if (plotCleanupService.cleanupPlot(plot, player.getUniqueId())) {
            messages.send(player, "city.plot.cleanup.started", "",
                    Map.of("id", plot.id()));
        }
    }

    private void handlePlotGoto(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.plot.goto.usage", "");
            return;
        }
        String reference = args[2];
        Optional<Plot> plotOptional = resolvePlotReference(reference);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", reference));
            return;
        }
        Plot plot = plotOptional.get();
        World world = Bukkit.getWorld(plot.world());
        if (world == null) {
            messages.send(player, "city.plot.goto.invalid-world", "", Map.of("world", plot.world()));
            return;
        }
        int centerX = (plot.x1() + plot.x2()) / 2;
        int centerZ = (plot.z1() + plot.z2()) / 2;
        int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
        Location target = new Location(world, centerX + 0.5, y, centerZ + 0.5);
        player.teleport(target);
        messages.send(player, "city.plot.goto.success", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("number", String.valueOf(plot.number())),
                        Map.entry("world", world.getName())));
    }

    private void handleShopSet(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!config.shopPlotsEnabled()) {
            messages.send(player, "city.plot.type-disabled", "",
                    Map.of("type", PlotType.SHOP.displayName()));
            return;
        }
        var result = player.rayTraceBlocks(6.0);
        if (result == null || result.getHitBlock() == null) {
            messages.send(player, "city.shop.set.no-target", "");
            return;
        }
        Location targetLocation = result.getHitBlock().getLocation();
        Optional<Plot> plotOptional = plotService.findContaining(targetLocation);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.shop.set.not-in-plot", "");
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.type() != PlotType.SHOP) {
            messages.send(player, "city.shop.set.not-shop-plot", "");
            return;
        }
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.shop.set.not-owned", "", Map.of("id", plot.id()));
            return;
        }
        if (!isOwnerTrustedOrAdmin(player, plot)) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        if (shopRentService != null && shopRentService.isRentEnabled()
                && shopRentService.isCommerceBlocked(plot, System.currentTimeMillis())) {
            messages.send(player, "city.shop.rent.blocked", "", Map.of("id", plot.id()));
            return;
        }
        if (shopPointService.isShopPoint(targetLocation)) {
            messages.send(player, "city.shop.set.already-exists", "");
            return;
        }
        int limit = Math.max(1, config.shopPointsPerPlot());
        int current = shopPointService.countByPlot(plot.id());
        if (current >= limit) {
            messages.send(player, "city.shop.set.limit-reached", "",
                    Map.of("limit", String.valueOf(limit)));
            return;
        }
        ShopPoint point = shopPointService.create(plot, targetLocation, plot.ownerUuid());
        messages.send(player, "city.shop.set.success", "",
                Map.ofEntries(
                        Map.entry("id", point.id()),
                        Map.entry("plot", plot.id())));
    }

    private void handleShopRemove(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length >= 3) {
            String id = args[2];
            Optional<ShopPoint> pointOptional = shopPointService.findById(id);
            if (pointOptional.isEmpty()) {
                messages.send(player, "city.shop.remove.not-found", "", Map.of("id", id));
                return;
            }
            ShopPoint point = pointOptional.get();
            Optional<Plot> plotOptional = plotRepository.findById(point.plotId());
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.shop.remove.not-found", "", Map.of("id", id));
                return;
            }
            Plot plot = plotOptional.get();
            if (plot.type() != PlotType.SHOP) {
                messages.send(player, "city.shop.set.not-shop-plot", "");
                return;
            }
            if (!isOwnerTrustedOrAdmin(player, plot)) {
                messages.send(player, "city.no-permission", "");
                return;
            }
            shopPointService.remove(point.id());
            messages.send(player, "city.shop.remove.success", "", Map.of("id", point.id()));
            return;
        }
        Optional<ShopPoint> pointOptional = shopPointService.findNearest(player.getLocation(), 2.0);
        if (pointOptional.isEmpty()) {
            messages.send(player, "city.shop.remove.nearby-none", "");
            return;
        }
        ShopPoint point = pointOptional.get();
        Optional<Plot> plotOptional = plotRepository.findById(point.plotId());
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.shop.remove.nearby-none", "");
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.type() != PlotType.SHOP) {
            messages.send(player, "city.shop.set.not-shop-plot", "");
            return;
        }
        if (!isOwnerTrustedOrAdmin(player, plot)) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        shopPointService.remove(point.id());
        messages.send(player, "city.shop.remove.success", "", Map.of("id", point.id()));
    }

    private void handleShopList(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.shop.list.usage", "");
            return;
        }
        String reference = args[2];
        Optional<Plot> plotOptional = resolvePlotReference(reference);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", reference));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.type() != PlotType.SHOP) {
            messages.send(player, "city.shop.set.not-shop-plot", "");
            return;
        }
        if (!isOwnerTrustedOrAdmin(player, plot)) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        List<ShopPoint> points = shopPointService.listByPlot(plot.id());
        if (points.isEmpty()) {
            messages.send(player, "city.shop.list.empty", "", Map.of("id", plot.id()));
            return;
        }
        messages.send(player, "city.shop.list.header", "", Map.of("id", plot.id()));
        for (ShopPoint point : points) {
            messages.send(player, "city.shop.list.line", "",
                    Map.ofEntries(
                            Map.entry("id", point.id()),
                            Map.entry("world", point.world()),
                            Map.entry("x", String.valueOf(point.x())),
                            Map.entry("y", String.valueOf(point.y())),
                            Map.entry("z", String.valueOf(point.z())),
                            Map.entry("enabled", String.valueOf(point.enabled()))));
        }
    }

    private void handleShopInfo(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        Optional<ShopPoint> pointOptional = shopPointService.findNearest(player.getLocation(), 2.0);
        if (pointOptional.isEmpty()) {
            messages.send(player, "city.shop.info.none", "");
            return;
        }
        ShopPoint point = pointOptional.get();
        Optional<Plot> plotOptional = plotRepository.findById(point.plotId());
        String plotId = plotOptional.map(Plot::id).orElse(point.plotId());
        String owner = plotOptional.map(Plot::ownerUuid)
                .map(this::formatOwner)
                .orElse(messages.getRaw("city.plot.owner.none", ""));
        messages.send(player, "city.shop.info.line", "",
                Map.ofEntries(
                        Map.entry("id", point.id()),
                        Map.entry("plot", plotId),
                        Map.entry("owner", owner),
                        Map.entry("enabled", String.valueOf(point.enabled()))));
    }

    private void handleShopRent(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "city.shop.rent.usage", "");
            return;
        }
        if (shopRentService == null || !shopRentService.isRentEnabled()) {
            messages.send(sender, "city.shop.rent.disabled", "");
            return;
        }
        String action = args[2].toLowerCase();
        switch (action) {
            case "status" -> handleShopRentStatus(sender, args);
            case "pay" -> handleShopRentPay(sender, args);
            default -> messages.send(sender, "city.shop.rent.usage", "");
        }
    }

    private void handleShopRentStatus(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "city.shop.rent.status.usage", "");
            return;
        }
        String reference = args[3];
        Optional<Plot> plotOptional = resolvePlotReference(reference);
        if (plotOptional.isEmpty()) {
            messages.send(sender, "city.plot.not-found", "", Map.of("id", reference));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.type() != PlotType.SHOP) {
            messages.send(sender, "city.shop.rent.not-shop", "");
            return;
        }
        long now = System.currentTimeMillis();
        boolean overdue = shopRentService.isRentOverdue(plot, now);
        boolean blocked = shopRentService.isCommerceBlocked(plot, now);
        String paidUntil = formatTimestamp(plot.rentPaidUntil());
        messages.send(sender, "city.shop.rent.status.line", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("paidUntil", paidUntil),
                        Map.entry("overdue", String.valueOf(overdue)),
                        Map.entry("blocked", String.valueOf(blocked))));
    }

    private void handleShopRentPay(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.shop.rent.pay.usage", "");
            return;
        }
        if (!shopRentService.isEconomyAvailable()) {
            messages.send(player, "city.shop.rent.no-economy", "");
            return;
        }
        String reference = args[3];
        Optional<Plot> plotOptional = resolvePlotReference(reference);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "", Map.of("id", reference));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.type() != PlotType.SHOP) {
            messages.send(player, "city.shop.rent.not-shop", "");
            return;
        }
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.shop.rent.not-owned", "", Map.of("id", plot.id()));
            return;
        }
        if (!isOwnerTrustedOrAdmin(player, plot)) {
            messages.send(player, "city.no-permission", "");
            return;
        }
        int periods = 1;
        if (args.length >= 5) {
            try {
                periods = Math.max(1, Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                messages.send(player, "city.shop.rent.pay.invalid-periods", "");
                return;
            }
        }
        int price = config.shopRentPricePerPeriod() * periods;
        if (!economyService.has(player, price)) {
            messages.send(player, "city.shop.rent.not-enough-money", "");
            return;
        }
        if (!economyService.withdraw(player, price)) {
            messages.send(player, "city.shop.rent.not-enough-money", "");
            return;
        }
        Plot updated = shopRentService.applyPayment(plot, periods);
        plotRepository.upsert(updated);
        shopRentService.onPaymentApplied(plot.id());
        messages.send(player, "city.shop.rent.pay.success", "",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
                        Map.entry("paidUntil", formatTimestamp(updated.rentPaidUntil())),
                        Map.entry("price", String.valueOf(price))));
    }

    private Optional<Plot> resolvePlotReference(String reference) {
        if (reference == null || reference.isBlank()) {
            return Optional.empty();
        }
        String trimmed = reference.trim();
        if (trimmed.startsWith("#")) {
            return parsePlotNumber(trimmed.substring(1))
                    .flatMap(plotRepository::findByNumber);
        }
        if (isNumber(trimmed)) {
            int number = Integer.parseInt(trimmed);
            Optional<Plot> byNumber = plotRepository.findByNumber(number);
            if (byNumber.isPresent()) {
                return byNumber;
            }
        }
        return plotRepository.findById(trimmed);
    }

    private Optional<Integer> parsePlotNumber(String raw) {
        if (raw == null || raw.isBlank() || !isNumber(raw)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private boolean isNumber(String raw) {
        return raw != null && raw.matches("\\d+");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "city.only-players", "");
        return null;
    }

    private boolean isOwnerOrAdmin(Player player, Plot plot) {
        return player.hasPermission(ADMIN_PERMISSION) || player.getUniqueId().equals(plot.ownerUuid());
    }

    private boolean isOwnerTrustedOrAdmin(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        if (plot.ownerUuid() != null && plot.ownerUuid().equals(player.getUniqueId())) {
            return true;
        }
        return plotMemberRepository
                .findRole(plot.id(), player.getUniqueId())
                .map(role -> role == PlotMemberRole.TRUSTED)
                .orElse(false);
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return messages.getRaw("city.shop.rent.never", "");
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(formatter);
    }

    private String formatOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return messages.getRaw("city.plot.owner.none", "");
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(ownerUuid);
        if (offline.getName() != null) {
            return offline.getName();
        }
        return ownerUuid.toString();
    }

    private double distanceToPlot(Location location, Plot plot) {
        double centerX = (plot.x1() + plot.x2()) / 2.0;
        double centerZ = (plot.z1() + plot.z2()) / 2.0;
        double dx = location.getX() - centerX;
        double dz = location.getZ() - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void sendUsage(CommandSender sender) {
        messages.send(sender, "city.usage", "");
    }

    private record PlotDistance(Plot plot, double distance) {
    }
}
