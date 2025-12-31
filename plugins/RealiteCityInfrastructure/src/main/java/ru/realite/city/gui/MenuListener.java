package ru.realite.city.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public final class MenuListener implements Listener {

    private final GuiService guiService;
    private final MenuFactory menuFactory;

    public MenuListener(GuiService guiService, MenuFactory menuFactory) {
        this.guiService = guiService;
        this.menuFactory = menuFactory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }

        var actionOptional = menuFactory.extractAction(event.getCurrentItem());
        if (actionOptional.isEmpty()) {
            return;
        }
        String action = actionOptional.get();

        switch (action) {
            case "open_selection" -> guiService.openSelection(player);
            case "open_plots" -> guiService.openPlots(player, 0);
            case "open_main" -> guiService.openMain(player);
            case "selection_pos1" -> guiService.handleSelectionPos1(player);
            case "selection_pos2" -> guiService.handleSelectionPos2(player);
            case "selection_clear" -> guiService.handleSelectionClear(player);
            case "plots_prev" -> guiService.handlePlotsPrev(player);
            case "plots_next" -> guiService.handlePlotsNext(player);
            case "plot_delete", "plot_delete_confirm" -> guiService.handleDeletePlot(player);
            case "plot_teleport" -> guiService.handleTeleport(player);
            case "plot_show_border" -> guiService.handlePlotShowBorder(player);
            case "plot_set_owner_player" -> guiService.handlePlotSetOwnerPlayer(player);
            case "plot_set_owner_guild" -> guiService.handlePlotSetOwnerGuild(player);
            case "player_open_access" -> guiService.openPlayerAccess(player, 0);
            case "player_info" -> guiService.handlePlayerInfo(player);
            case "player_show_border" -> guiService.handlePlayerShowBorder(player);
            case "player_teleport" -> guiService.handlePlayerTeleport(player);
            case "player_back" -> guiService.openPlayerMain(player);
            case "player_access_prev" -> guiService.handlePlayerAccessPrev(player);
            case "player_access_next" -> guiService.handlePlayerAccessNext(player);
            case "player_access_add" -> guiService.handlePlayerAccessAdd(player);
            case "player_access_remove_all" -> guiService.handlePlayerAccessRemoveAll(player);
            case "player_access_remove" -> menuFactory.extractMemberId(event.getCurrentItem())
                    .map(java.util.UUID::fromString)
                    .ifPresent(memberId -> guiService.handlePlayerAccessRemove(player, memberId));
            case "open_plot_actions" -> menuFactory.extractPlotId(event.getCurrentItem())
                    .ifPresent(plotId -> guiService.openPlotActions(player, plotId));
            default -> {
            }
        }
    }
}
