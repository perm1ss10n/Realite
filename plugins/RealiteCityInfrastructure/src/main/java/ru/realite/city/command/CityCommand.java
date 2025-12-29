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
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.service.CityAreaSelectionService.Selection;
import ru.realite.city.service.PlotCleanupService;
import ru.realite.city.service.PlotService;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    public CityCommand(
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            PlotService plotService,
            PlotCleanupService plotCleanupService,
            CityAreaSelectionService selectionService,
            CityMessages messages,
            CityConfig config) {
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.plotService = plotService;
        this.plotCleanupService = plotCleanupService;
        this.selectionService = selectionService;
        this.messages = messages;
        this.config = config;
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
            messages.send(sender, "city.no-permission", "&cYou do not have permission.");
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
                messages.send(player, "city.area.wand-enabled", "&aCity area wand enabled.");
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
            case "addmember" -> handlePlotAddMember(sender, args);
            case "trust" -> handlePlotAddMember(sender, args);
            case "removemember" -> handlePlotRemoveMember(sender, args);
            case "untrust" -> handlePlotRemoveMember(sender, args);
            case "members" -> handlePlotMembers(sender, args);
            case "transfer" -> handlePlotTransfer(sender, args);
            case "sell" -> handlePlotSell(sender, args);
            case "accept" -> handlePlotAccept(sender, args);
            case "release" -> handlePlotRelease(sender, args);
            default -> sendUsage(sender);
        }
    }

    private void handleCreateArea(Player player, String[] args) {
        if (args.length < 3) {
            messages.send(player, "city.area.create.usage", "&eUsage: /city area create <id>");
            return;
        }
        String id = args[2];
        Optional<CityArea> existing = cityAreaRepository.findById(id);
        if (existing.isPresent()) {
            messages.send(player, "city.area.exists", "&cCity area {id} already exists.", Map.of("id", id));
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            messages.send(player, "city.area.selection-missing", "&cBoth pos1 and pos2 must be set.");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "city.area.invalid-world", "&cInvalid selection world.");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(player, "city.area.mismatched-world", "&cpos1 and pos2 must be in the same world.");
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
        messages.send(player, "city.area.created", "&aCity area {id} created.", Map.of("id", id));
    }

    private void handleDeleteArea(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "city.area.delete.usage", "&eUsage: /city area delete <id>");
            return;
        }
        String id = args[2];
        if (cityAreaRepository.delete(id)) {
            messages.send(sender, "city.area.deleted", "&aCity area {id} deleted.", Map.of("id", id));
        } else {
            messages.send(sender, "city.area.not-found", "&cCity area {id} not found.", Map.of("id", id));
        }
    }

    private void handleListAreas(CommandSender sender) {
        List<CityArea> areas = cityAreaRepository.findAll();
        if (areas.isEmpty()) {
            messages.send(sender, "city.area.none", "&eNo city areas defined.");
            return;
        }
        areas.sort(Comparator.comparing(CityArea::id));
        messages.send(sender, "city.area.list.header", "&eCity areas:");
        for (CityArea area : areas) {
            messages.send(sender, "city.area.list.line",
                    "- {id} ({world}) [{minX},{minY},{minZ}] -> [{maxX},{maxY},{maxZ}]",
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
            messages.send(sender, "city.plot.list.none", "&eNo plots defined.");
            return;
        }
        plots.sort(Comparator.comparing(Plot::id));
        messages.send(sender, "city.plot.list.header", "Plots:");
        for (Plot plot : plots) {
            messages.send(sender, "city.plot.list.line", "{line}", Map.of("line", formatPlotLine(plot)));
        }
    }

    private void handlePlotInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "city.plot.info.usage", "&eUsage: /city plot info <id>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(sender, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        messages.send(sender, "city.plot.info", "&ePlot {id}: &7type={type} price={price} owner={owner}",
                Map.ofEntries(
                        Map.entry("id", plot.id()),
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
        String id;
        if (args.length < 3) {
            Optional<Plot> plotOptional = plotService.findContaining(player.getLocation());
            if (plotOptional.isEmpty()) {
                messages.send(player, "city.plot.buy.not-in-plot",
                        "&cYou are not inside a plot. Use /city plot nearby or /city plot buy <id>.");
                return;
            }
            id = plotOptional.get().id();
        } else {
            id = args[2];
        }
        PlotService.BuyResult result = plotService.buyPlot(player, id);
        switch (result) {
            case SUCCESS -> messages.send(player, "city.plot.buy.success", "&aPlot {id} bought.", Map.of("id", id));
            case NOT_FOUND -> messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            case ALREADY_OWNED -> messages.send(player, "city.plot.buy.already-owned", "&cPlot {id} is already owned.",
                    Map.of("id", id));
            case TYPE_DISABLED -> messages.send(player, "city.plot.type-disabled", "&cPlot type {type} is disabled.",
                    Map.of("type",
                            plotRepository.findById(id).map(plot -> plot.type().displayName()).orElse("unknown")));
            case LIMIT_REACHED -> messages.send(player, "city.plot.limit-reached", "&cPlot limit reached.",
                    Map.of("limit", String.valueOf(config.defaultPlotsPerPlayer())));
            case NO_ECONOMY -> messages.send(player, "city.plot.no-economy", "&cEconomy not configured.");
        }
    }

    private void handlePlotNearby(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        int radius = 100;
        if (args.length >= 3) {
            try {
                radius = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                messages.send(player, "city.plot.nearby.usage", "&eUsage: /city plot nearby [radius]");
                return;
            }
        }

        final int r = radius;

        Location location = player.getLocation();
        if (location.getWorld() == null) {
            messages.send(player, "city.plot.nearby.none", "&eNo free plots nearby.");
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
            messages.send(player, "city.plot.nearby.none", "&eNo free plots nearby.");
            return;
        }
        messages.send(player, "city.plot.nearby.header", "&eNearby free plots:");
        for (PlotDistance distance : nearby) {
            Plot plot = distance.plot();
            messages.send(player, "city.plot.nearby.line", "{id} ({type}) price={price} dist={dist}",
                    Map.ofEntries(
                            Map.entry("id", plot.id()),
                            Map.entry("type", plot.type().displayName()),
                            Map.entry("price", String.valueOf(plot.price())),
                            Map.entry("dist", String.valueOf(Math.round(distance.distance())))));
        }
    }

    private void handlePlotCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "city.no-permission", "&cYou do not have permission.");
            return;
        }
        if (args.length < 5) {
            messages.send(player, "city.plot.create.usage", "&eUsage: /city plot create <id> <home|shop> <price>");
            return;
        }
        String id = args[2];
        PlotType type = PlotType.fromToken(args[3]);
        if (type == null) {
            messages.send(player, "city.plot.create.unknown-type", "&cUnknown plot type.");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            messages.send(player, "city.plot.create.invalid-price", "&cInvalid price.");
            return;
        }
        if (plotRepository.findById(id).isPresent()) {
            messages.send(player, "city.plot.create.exists", "&cPlot {id} already exists.", Map.of("id", id));
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            messages.send(player, "city.area.selection-missing", "&cBoth pos1 and pos2 must be set.");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "city.area.invalid-world", "&cInvalid selection world.");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            messages.send(player, "city.area.mismatched-world", "&cpos1 and pos2 must be in the same world.");
            return;
        }
        World world = pos1.getWorld();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        Plot plot = new Plot(
                id,
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
                System.currentTimeMillis());
        plotRepository.upsert(plot);
        messages.send(player, "city.plot.created", "&aPlot {id} created.", Map.of("id", id));
    }

    private void handlePlotDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            messages.send(sender, "city.no-permission", "&cYou do not have permission.");
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "city.plot.delete.usage", "&eUsage: /city plot delete <id>");
            return;
        }
        String id = args[2];
        if (plotRepository.delete(id)) {
            messages.send(sender, "city.plot.deleted", "&aPlot {id} deleted.", Map.of("id", id));
        } else {
            messages.send(sender, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
        }
    }

    private void handlePlotAddMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.plot.member.add.usage", "&eUsage: /city plot addmember <id> <player>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.member.self", "&cYou cannot add yourself.");
            return;
        }
        plotMemberRepository.upsert(id, targetId, PlotMemberRole.MEMBER);
        messages.send(player, "city.plot.member.added", "&aAdded {player} to plot {id}.",
                Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
    }

    private void handlePlotRemoveMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            messages.send(player, "city.plot.member.remove.usage", "&eUsage: /city plot removemember <id> <player>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (plotMemberRepository.remove(id, targetId)) {
            messages.send(player, "city.plot.member.removed", "&aRemoved {player} from plot {id}.",
                    Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
        } else {
            messages.send(player, "city.plot.member.not-found", "&cPlayer is not a plot member.");
        }
    }

    private void handlePlotMembers(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.plot.members.usage", "&eUsage: /city plot members <id>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        Map<UUID, PlotMemberRole> members = plotMemberRepository.findMembers(id);
        messages.send(player, "city.plot.members.header", "&ePlot {id} members:", Map.of("id", id));
        if (members.isEmpty()) {
            messages.send(player, "city.plot.members.empty", "&7(no members)");
            return;
        }
        for (Map.Entry<UUID, PlotMemberRole> entry : members.entrySet()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(entry.getKey());
            String name = member.getName() == null ? entry.getKey().toString() : member.getName();
            messages.send(player, "city.plot.members.line", "- {player} ({role})",
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
            messages.send(player, "city.plot.transfer.usage", "&eUsage: /city plot transfer <id> <player>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.transfer.self", "&cYou cannot transfer to yourself.");
            return;
        }
        long expiresAt = System.currentTimeMillis() + 120_000L;
        plotService.createTransferOffer(id, targetId, expiresAt);
        messages.send(player, "city.plot.transfer.sent", "&aTransfer offer sent to {player} for plot {id}.",
                Map.ofEntries(
                        Map.entry("player", target.getName() == null ? targetId.toString() : target.getName()),
                        Map.entry("id", id)));
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                messages.send(online, "city.plot.transfer.sent",
                        "&eTransfer offer for plot {id} from {player}.",
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
            messages.send(player, "city.plot.sell.usage", "&eUsage: /city plot sell <id> <player> <price>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            messages.send(player, "city.plot.sell.invalid-price", "&cInvalid price.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
        UUID targetId = target.getUniqueId();
        if (player.getUniqueId().equals(targetId)) {
            messages.send(player, "city.plot.sell.self", "&cYou cannot sell to yourself.");
            return;
        }
        long expiresAt = System.currentTimeMillis() + 120_000L;
        plotService.createSellOffer(id, targetId, expiresAt, price);
        messages.send(player, "city.plot.sell.sent",
                "&aSell offer sent to {player} for plot {id} (price {price}).",
                Map.ofEntries(
                        Map.entry("player", target.getName() == null ? targetId.toString() : target.getName()),
                        Map.entry("id", id),
                        Map.entry("price", String.valueOf(price))));
        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                messages.send(online, "city.plot.sell.sent",
                        "&eSell offer for plot {id} from {player} (price {price}).",
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
            messages.send(player, "city.plot.accept.usage", "&eUsage: /city plot accept <id>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
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
        messages.send(player, "city.plot.transfer.expired", "&cTransfer offer expired for plot {id}.",
                Map.of("id", id));
    }

    private void handleTransferAccept(Player player, Plot plot, PlotService.PendingTransfer pending) {
        if (!player.getUniqueId().equals(pending.targetUuid())) {
            messages.send(player, "city.no-permission", "&cYou do not have permission.");
            return;
        }
        if (plotRepository.countOwned(player.getUniqueId(), null) >= config.defaultPlotsPerPlayer()) {
            messages.send(player, "city.plot.transfer.limit-reached", "&cPlot limit reached.",
                    Map.of("limit", String.valueOf(config.defaultPlotsPerPlayer())));
            return;
        }
        plotService.clearPendingOffers(plot.id());
        Plot updated = new Plot(
                plot.id(),
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
                plot.createdAt());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        messages.send(player, "city.plot.transfer.accepted", "&aPlot {id} transferred to you.",
                Map.of("id", plot.id()));
    }

    private void handleSellAccept(Player player, Plot plot, PlotService.PendingSell pending) {
        if (!player.getUniqueId().equals(pending.targetUuid())) {
            messages.send(player, "city.no-permission", "&cYou do not have permission.");
            return;
        }
        if (pending.price() > 0) {
            messages.send(player, "city.plot.sell.no-economy", "&cEconomy not configured.");
            return;
        }
        if (plotRepository.countOwned(player.getUniqueId(), null) >= config.defaultPlotsPerPlayer()) {
            messages.send(player, "city.plot.transfer.limit-reached", "&cPlot limit reached.",
                    Map.of("limit", String.valueOf(config.defaultPlotsPerPlayer())));
            return;
        }
        plotService.clearPendingOffers(plot.id());
        Plot updated = new Plot(
                plot.id(),
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
                plot.createdAt());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        messages.send(player, "city.plot.transfer.accepted", "&aPlot {id} transferred to you.",
                Map.of("id", plot.id()));
    }

    private void handlePlotRelease(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 3) {
            messages.send(player, "city.plot.release.usage", "&eUsage: /city plot release <id>");
            return;
        }
        String id = args[2];
        Optional<Plot> plotOptional = plotRepository.findById(id);
        if (plotOptional.isEmpty()) {
            messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            return;
        }
        Plot plot = plotOptional.get();
        if (plot.ownerUuid() == null) {
            messages.send(player, "city.plot.not-owned", "&cPlot {id} is not owned.", Map.of("id", id));
            return;
        }
        if (!isOwnerOrAdmin(player, plot)) {
            messages.send(player, "city.plot.not-owner", "&cYou are not the owner of this plot.");
            return;
        }
        Plot updated = new Plot(
                plot.id(),
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
                plot.createdAt());
        plotRepository.upsert(updated);
        plotMemberRepository.removeAll(plot.id());
        plotService.clearPendingOffers(plot.id());
        messages.send(player, "city.plot.release.success", "&aPlot {id} released.", Map.of("id", plot.id()));
        if (plotCleanupService.cleanupPlot(plot, player.getUniqueId())) {
            messages.send(player, "city.plot.cleanup.started", "&ePlot cleanup started.",
                    Map.of("id", plot.id()));
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        messages.send(sender, "city.only-players", "&cOnly players can use this command.");
        return null;
    }

    private boolean isOwnerOrAdmin(Player player, Plot plot) {
        return player.hasPermission(ADMIN_PERMISSION) || player.getUniqueId().equals(plot.ownerUuid());
    }

    private String formatOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return "none";
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(ownerUuid);
        if (offline.getName() != null) {
            return offline.getName();
        }
        return ownerUuid.toString();
    }

    private String formatPlotLine(Plot plot) {
        return plot.id()
                + " (" + plot.type().displayName() + ") "
                + "price=" + plot.price()
                + " owner=" + formatOwner(plot.ownerUuid());
    }

    private double distanceToPlot(Location location, Plot plot) {
        double centerX = (plot.x1() + plot.x2()) / 2.0;
        double centerZ = (plot.z1() + plot.z2()) / 2.0;
        double dx = location.getX() - centerX;
        double dz = location.getZ() - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void sendUsage(CommandSender sender) {
        messages.send(sender, "city.usage", "&eUsage: /city <area|plot> <...>");
    }

    private record PlotDistance(Plot plot, double distance) {
    }
}
