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
    private final CityAreaSelectionService selectionService;
    private final CityMessages messages;
    private final CityConfig config;

    public CityCommand(
            CityAreaRepository cityAreaRepository,
            PlotRepository plotRepository,
            PlotMemberRepository plotMemberRepository,
            PlotService plotService,
            CityAreaSelectionService selectionService,
            CityMessages messages,
            CityConfig config) {
        this.cityAreaRepository = cityAreaRepository;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
        this.plotService = plotService;
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
            sender.sendMessage("You do not have permission to use this command.");
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
                player.sendMessage("City area wand enabled.");
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
            case "create" -> handlePlotCreate(sender, args);
            case "delete" -> handlePlotDelete(sender, args);
            case "addmember" -> handlePlotAddMember(sender, args);
            case "removemember" -> handlePlotRemoveMember(sender, args);
            default -> sendUsage(sender);
        }
    }

    private void handleCreateArea(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /city area create <id>");
            return;
        }
        String id = args[2];
        Optional<CityArea> existing = cityAreaRepository.findById(id);
        if (existing.isPresent()) {
            player.sendMessage("CityArea " + id + " already exists.");
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            player.sendMessage("Both pos1 and pos2 must be set.");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage("Invalid selection world.");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("pos1 and pos2 must be in the same world.");
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
        player.sendMessage("CityArea " + id + " created.");
    }

    private void handleDeleteArea(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /city area delete <id>");
            return;
        }
        String id = args[2];
        if (cityAreaRepository.delete(id)) {
            sender.sendMessage("CityArea " + id + " deleted.");
        } else {
            sender.sendMessage("CityArea " + id + " not found.");
        }
    }

    private void handleListAreas(CommandSender sender) {
        List<CityArea> areas = cityAreaRepository.findAll();
        if (areas.isEmpty()) {
            sender.sendMessage("No city areas defined.");
            return;
        }
        areas.sort(Comparator.comparing(CityArea::id));
        sender.sendMessage("City areas:");
        for (CityArea area : areas) {
            sender.sendMessage("- " + area.id()
                    + " (" + area.world() + ") "
                    + "[" + area.minX() + "," + area.minY() + "," + area.minZ()
                    + "] -> [" + area.maxX() + "," + area.maxY() + "," + area.maxZ() + "]");
        }
    }

    private void handleListPlots(CommandSender sender) {
        List<Plot> plots = plotRepository.findAll();
        if (plots.isEmpty()) {
            sender.sendMessage("No plots defined.");
            return;
        }
        plots.sort(Comparator.comparing(Plot::id));
        messages.send(sender, "city.plot.list.header", "Plots:");
        for (Plot plot : plots) {
            sender.sendMessage(formatPlotLine(plot));
        }
    }

    private void handlePlotInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /city plot info <id>");
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
        if (args.length < 3) {
            player.sendMessage("Usage: /city plot buy <id>");
            return;
        }
        String id = args[2];
        PlotService.BuyResult result = plotService.buyPlot(player, id);
        switch (result) {
            case SUCCESS -> messages.send(player, "city.plot.bought", "&aPlot {id} bought.", Map.of("id", id));
            case NOT_FOUND -> messages.send(player, "city.plot.not-found", "&cPlot {id} not found.", Map.of("id", id));
            case ALREADY_OWNED -> messages.send(player, "city.plot.already-owned", "&cPlot {id} is already owned.",
                    Map.of("id", id));
            case TYPE_DISABLED -> messages.send(player, "city.plot.type-disabled", "&cPlot type {type} is disabled.",
                    Map.of("type",
                            plotRepository.findById(id).map(plot -> plot.type().displayName()).orElse("unknown")));
            case LIMIT_REACHED -> messages.send(player, "city.plot.limit-reached", "&cPlot limit reached.",
                    Map.of("limit", String.valueOf(config.defaultPlotsPerPlayer())));
            case NO_ECONOMY -> messages.send(player, "city.plot.no-economy", "&cEconomy not configured.");
        }
    }

    private void handlePlotCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        if (args.length < 5) {
            player.sendMessage("Usage: /city plot create <id> <home|shop> <price>");
            return;
        }
        String id = args[2];
        PlotType type = PlotType.fromToken(args[3]);
        if (type == null) {
            player.sendMessage("Unknown plot type.");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage("Invalid price.");
            return;
        }
        if (plotRepository.findById(id).isPresent()) {
            player.sendMessage("Plot " + id + " already exists.");
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            player.sendMessage("Both pos1 and pos2 must be set.");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage("Invalid selection world.");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("pos1 and pos2 must be in the same world.");
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
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /city plot delete <id>");
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
            player.sendMessage("Usage: /city plot addmember <id> <player>");
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
        plotMemberRepository.upsert(id, targetId, PlotMemberRole.MEMBER);
        messages.send(player, "city.plot.member-added", "&aAdded {player} to plot {id}.",
                Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
    }

    private void handlePlotRemoveMember(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 4) {
            player.sendMessage("Usage: /city plot removemember <id> <player>");
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
            messages.send(player, "city.plot.member-removed", "&aRemoved {player} from plot {id}.",
                    Map.of("player", target.getName() == null ? targetId.toString() : target.getName(), "id", id));
        } else {
            player.sendMessage("Player is not a plot member.");
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage("Only players can use this command.");
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
        return "- " + plot.id()
                + " (" + plot.type().displayName() + ") "
                + "price=" + plot.price()
                + " owner=" + formatOwner(plot.ownerUuid());
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("Usage: /city <area|plot> <...>");
    }
}
