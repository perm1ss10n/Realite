package ru.realite.city.gui;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.Plot;
import ru.realite.city.service.AccessResult;
import ru.realite.city.service.CityAdminService;

import java.util.Map;
import java.util.Optional;

public final class GuiService {

    private final CityConfig config;
    private final CityMessages messages;
    private final CityAdminService adminService;
    private final GuiSessionStore sessionStore;
    private final MenuFactory menuFactory;

    public GuiService(CityConfig config,
                      CityMessages messages,
                      CityAdminService adminService,
                      GuiSessionStore sessionStore,
                      MenuFactory menuFactory) {
        this.config = config;
        this.messages = messages;
        this.adminService = adminService;
        this.sessionStore = sessionStore;
        this.menuFactory = menuFactory;
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
        player.openInventory(menuFactory.adminPlotActions(player, plotOptional.get(), false));
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
                player.openInventory(menuFactory.adminPlotActions(player, plotOptional.get(), true));
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

    public boolean guiEnabled() {
        return config.guiEnabled();
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
