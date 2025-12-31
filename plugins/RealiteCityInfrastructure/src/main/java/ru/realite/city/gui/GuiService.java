package ru.realite.city.gui;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.model.PlotMemberRole;
import ru.realite.city.model.PlotOwnerType;
import ru.realite.city.service.AccessResult;
import ru.realite.city.service.ChatInputService;
import ru.realite.city.service.CityAdminService;
import ru.realite.city.service.PlotBorderVisualizationService;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GuiService {

    private static final String PLAYER_TELEPORT_PERMISSION = "realite.city.plot.teleport";
    private static final String ADMIN_PERMISSION = "realite.city.admin";

    private final CityConfig config;
    private final CityMessages messages;
    private final CityAdminService adminService;
    private final ChatInputService chatInputService;
    private final PlotBorderVisualizationService plotBorderVisualizationService;
    private final GuiSessionStore sessionStore;
    private final MenuFactory menuFactory;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;

    public GuiService(CityConfig config,
                      CityMessages messages,
                      CityAdminService adminService,
                      ChatInputService chatInputService,
                      PlotBorderVisualizationService plotBorderVisualizationService,
                      GuiSessionStore sessionStore,
                      MenuFactory menuFactory,
                      PlotRepository plotRepository,
                      PlotMemberRepository plotMemberRepository) {
        this.config = config;
        this.messages = messages;
        this.adminService = adminService;
        this.chatInputService = chatInputService;
        this.plotBorderVisualizationService = plotBorderVisualizationService;
        this.sessionStore = sessionStore;
        this.menuFactory = menuFactory;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
    }

    public void openMain(Player player) {
        if (player == null) {
            return;
        }
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.ADMIN_MAIN);
        session.deleteConfirmation(false);
        player.openInventory(menuFactory.adminMain());
    }

    public void openSelection(Player player) {
        if (player == null) {
            return;
        }
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.ADMIN_SELECTION);
        session.deleteConfirmation(false);
        player.openInventory(menuFactory.adminSelection(player));
    }

    public void openPlots(Player player, int page) {
        if (player == null) {
            return;
        }
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.ADMIN_PLOTS);
        int safePage = menuFactory.clampPlotsPage(page);
        session.plotsPage(safePage);
        session.deleteConfirmation(false);
        player.openInventory(menuFactory.adminPlots(player, session.plotsPage()));
    }

    public void openPlotActions(Player player, String plotId) {
        if (player == null) {
            return;
        }
        Optional<Plot> plotOptional = adminService.findPlot(plotId);
        if (plotOptional.isEmpty()) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", String.valueOf(plotId)))));
            return;
        }
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.ADMIN_PLOT_ACTIONS);
        session.selectedPlotId(plotId);
        session.deleteConfirmation(false);
        player.openInventory(menuFactory.adminPlotActions(
                player,
                plotOptional.get(),
                false,
                config.plotsSetOwnerAllowViaGui()));
    }

    public void handleSelectionPos1(Player player) {
        if (!handleAccess(player, adminService.setSelectionPos1(player), null)) {
            return;
        }
        playClick(player);
        openSelection(player);
    }

    public void handleSelectionPos2(Player player) {
        if (!handleAccess(player, adminService.setSelectionPos2(player), null)) {
            return;
        }
        playClick(player);
        openSelection(player);
    }

    public void handleSelectionClear(Player player) {
        if (!handleAccess(player, adminService.clearSelection(player), null)) {
            return;
        }
        playClick(player);
        openSelection(player);
    }

    public void handlePlotsPrev(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        int page = Math.max(0, session.plotsPage() - 1);
        openPlots(player, page);
    }

    public void handlePlotsNext(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        int page = session.plotsPage() + 1;
        openPlots(player, page);
    }

    public void handleDeletePlot(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        String plotId = session.selectedPlotId();
        if (plotId == null) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", "?"))));
            return;
        }
        if (!session.deleteConfirmation()) {
            session.deleteConfirmation(true);
            Optional<Plot> plotOptional = adminService.findPlot(plotId);
            if (plotOptional.isPresent()) {
                player.openInventory(menuFactory.adminPlotActions(
                        player,
                        plotOptional.get(),
                        true,
                        config.plotsSetOwnerAllowViaGui()));
            }
            return;
        }
        if (!handleAccess(player, adminService.deletePlot(player, plotId), Map.of("id", plotId))) {
            return;
        }
        session.deleteConfirmation(false);
        openPlots(player, session.plotsPage());
    }

    public void handleTeleport(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        String plotId = session.selectedPlotId();
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("id", plotId == null ? "?" : plotId);
        adminService.findPlot(plotId).ifPresent(plot -> vars.put("world", plot.world()));
        if (!handleAccess(player, adminService.teleportToPlot(player, plotId), vars)) {
            return;
        }
        playClick(player);
    }

    public void handleStubOwner(Player player) {
        handleAccess(player, adminService.stubSetOwner(player), null);
    }

    public void handlePlotSetOwnerPlayer(Player player) {
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(player, "gui.error.no_permission", "");
            return;
        }
        if (!config.plotsSetOwnerAllowViaGui()) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", messages.getRaw("plot.setowner.gui_disabled", "disabled")));
            return;
        }
        String plotId = sessionStore.getOrCreate(player.getUniqueId()).selectedPlotId();
        if (plotId == null) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", "?"))));
            return;
        }
        chatInputService.start(player, ChatInputService.Mode.SET_OWNER_PLAYER, plotId);
    }

    public void handlePlotSetOwnerGuild(Player player) {
        if (player == null) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            messages.send(player, "gui.error.no_permission", "");
            return;
        }
        if (!config.plotsSetOwnerAllowViaGui()) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", messages.getRaw("plot.setowner.gui_disabled", "disabled")));
            return;
        }
        String plotId = sessionStore.getOrCreate(player.getUniqueId()).selectedPlotId();
        if (plotId == null) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", "?"))));
            return;
        }
        chatInputService.start(player, ChatInputService.Mode.SET_OWNER_GUILD, plotId);
    }

    public boolean guiEnabled() {
        return config.guiEnabled();
    }

    public boolean playerGuiEnabled() {
        return config.playerGuiEnabled();
    }

    public boolean openPlayerMain(Player player) {
        if (player == null) {
            return false;
        }
        Optional<Plot> plotOptional = findPlayerPlot(player);
        if (plotOptional.isEmpty()) {
            messages.send(player, "gui.plot.none", "");
            return false;
        }
        Plot plot = plotOptional.get();
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.PLAYER_MAIN);
        session.selectedPlotId(plot.id());
        session.accessPage(0);
        session.deleteConfirmation(false);
        boolean canTeleport = player.hasPermission(PLAYER_TELEPORT_PERMISSION);
        player.openInventory(menuFactory.playerMain(player, plot, canTeleport));
        return true;
    }

    public void openPlayerAccess(Player player, int page) {
        if (player == null) {
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        List<UUID> trusted = plotMemberRepository.findMembers(plot.id()).entrySet().stream()
                .filter(entry -> entry.getValue() == PlotMemberRole.TRUSTED)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());
        int maxPage = Math.max(0, (trusted.size() - 1) / MenuFactory.TRUSTED_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        session.menu(MenuType.PLAYER_ACCESS);
        session.accessPage(safePage);
        session.selectedPlotId(plot.id());
        session.deleteConfirmation(false);
        boolean canAdd = config.trustedMax() <= 0 || trusted.size() < config.trustedMax();
        player.openInventory(menuFactory.playerAccess(player, plot, trusted, safePage, maxPage, canAdd));
    }

    public void handlePlayerAccessPrev(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        int page = Math.max(0, session.accessPage() - 1);
        openPlayerAccess(player, page);
    }

    public void handlePlayerAccessNext(Player player) {
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        int page = session.accessPage() + 1;
        openPlayerAccess(player, page);
    }

    public void handlePlayerAccessRemove(Player player, UUID memberId) {
        if (player == null || memberId == null) {
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        boolean removed = plotMemberRepository.remove(plot.id(), memberId);
        if (removed) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(memberId);
            String name = target.getName() == null ? memberId.toString() : target.getName();
            messages.send(player, "city.plot.member.removed", "",
                    Map.of("player", name, "id", plot.id()));
        } else {
            messages.send(player, "city.plot.member.not-found", "");
        }
        openPlayerAccess(player, sessionStore.getOrCreate(player.getUniqueId()).accessPage());
    }

    public void handlePlayerAccessAdd(Player player) {
        if (player == null) {
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        long trustedCount = plotMemberRepository.findMembers(plot.id()).values().stream()
                .filter(role -> role == PlotMemberRole.TRUSTED)
                .count();
        if (config.trustedMax() > 0 && trustedCount >= config.trustedMax()) {
            messages.send(player, "plot.trusted.limit", "", Map.of("limit", String.valueOf(config.trustedMax())));
            return;
        }
        chatInputService.start(player, ChatInputService.Mode.ADD_TRUSTED, plot.id());
    }

    public void handlePlayerInfo(Player player) {
        if (player == null) {
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        messages.send(player, "city.plot.info", "",
                Map.ofEntries(
                        Map.entry("number", String.valueOf(plot.number())),
                        Map.entry("id", plot.id()),
                        Map.entry("type", plot.type() == null ? "unknown" : plot.type().displayName()),
                        Map.entry("price", String.valueOf(plot.price())),
                        Map.entry("owner", formatOwner(plot)),
                        Map.entry("world", plot.world()),
                        Map.entry("x1", String.valueOf(plot.x1())),
                        Map.entry("y1", String.valueOf(plot.y1())),
                        Map.entry("z1", String.valueOf(plot.z1())),
                        Map.entry("x2", String.valueOf(plot.x2())),
                        Map.entry("y2", String.valueOf(plot.y2())),
                        Map.entry("z2", String.valueOf(plot.z2()))
                ));
    }

    public void handlePlayerShowBorder(Player player) {
        if (player == null) {
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        plotBorderVisualizationService.showBorder(player, plot);
    }

    public void handlePlayerTeleport(Player player) {
        if (player == null) {
            return;
        }
        if (!player.hasPermission(PLAYER_TELEPORT_PERMISSION)) {
            messages.send(player, "gui.error.no_permission", "");
            return;
        }
        Plot plot = resolvePlayerPlot(player);
        if (plot == null) {
            return;
        }
        var world = Bukkit.getWorld(plot.world());
        if (world == null) {
            messages.send(player, "city.plot.goto.invalid-world", "");
            return;
        }
        int centerX = (plot.x1() + plot.x2()) / 2;
        int centerZ = (plot.z1() + plot.z2()) / 2;
        int y = world.getHighestBlockYAt(centerX, centerZ) + 1;
        Location target = new Location(world, centerX + 0.5, y, centerZ + 0.5);
        player.teleport(target);
        playClick(player);
    }

    public void handlePlotShowBorder(Player player) {
        if (player == null) {
            return;
        }
        GuiSessionStore.GuiSession session = sessionStore.getOrCreate(player.getUniqueId());
        String plotId = session.selectedPlotId();
        if (plotId == null) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", "?"))));
            return;
        }
        Optional<Plot> plotOptional = adminService.findPlot(plotId);
        if (plotOptional.isEmpty()) {
            messages.send(player, "gui.error.action_failed", "",
                    Map.of("reason", formatReason("city.plot.not-found", Map.of("id", plotId))));
            return;
        }
        plotBorderVisualizationService.showBorder(player, plotOptional.get());
    }

    private boolean handleAccess(Player player, AccessResult result, Map<String, String> vars) {
        if (result == null) {
            return false;
        }
        if (result.isAllowed()) {
            return true;
        }
        String reasonKey = result.reasonKey();
        if (reasonKey != null && reasonKey.startsWith("gui.")) {
            messages.send(player, reasonKey, "");
            return false;
        }
        if (reasonKey != null) {
            String reason = formatReason(reasonKey, vars);
            messages.send(player, "gui.error.action_failed", "", Map.of("reason", reason));
            return false;
        }
        messages.send(player, "gui.error.action_failed", "", Map.of("reason", "unknown"));
        return false;
    }

    private Plot resolvePlayerPlot(Player player) {
        Optional<Plot> plotOptional = findPlayerPlot(player);
        if (plotOptional.isEmpty()) {
            messages.send(player, "gui.plot.none", "");
            return null;
        }
        return plotOptional.get();
    }

    private Optional<Plot> findPlayerPlot(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Optional<Plot> byLocation = plotRepository.findContaining(player.getLocation())
                .filter(plot -> plot.isOwnedByPlayer(player.getUniqueId()));
        if (byLocation.isPresent()) {
            return byLocation;
        }
        List<Plot> ownedPlots = plotRepository.findByOwner(player.getUniqueId());
        return ownedPlots.stream()
                .filter(plot -> plot.ownerType() == PlotOwnerType.PLAYER)
                .min(Comparator.comparingInt(Plot::number));
    }

    private String formatOwner(Plot plot) {
        if (plot == null || plot.ownerId() == null) {
            return messages.getRaw("city.plot.owner.none", "none");
        }
        if (plot.ownerType() == PlotOwnerType.PLAYER) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(plot.ownerId());
            return owner.getName() == null ? plot.ownerId().toString() : owner.getName();
        }
        return plot.ownerType().name().toLowerCase() + ":" + plot.ownerId();
    }

    private String formatReason(String reasonKey, Map<String, String> vars) {
        String reason = messages.getRaw(reasonKey, reasonKey);
        if (vars == null || vars.isEmpty()) {
            return reason;
        }
        for (var entry : vars.entrySet()) {
            reason = reason.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return reason;
    }

    private void playClick(Player player) {
        if (player == null) {
            return;
        }
        if (config.guiSoundsEnabled()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
        }
    }
}
