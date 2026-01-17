package ru.realite.ui.familiars.menu;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.core.api.familiars.FamiliarActionResult;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.menu.BaseMenu;

public final class FamiliarReleaseConfirmMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final MiniMessageMessages messages;
    private final FamiliarUiService uiService;
    private final UiScreenRegistry screenRegistry;
    private final String typeId;
    private final String displayName;
    private final String backTarget;

    public FamiliarReleaseConfirmMenu(MiniMessageMessages messages,
                                      FamiliarUiService uiService,
                                      UiScreenRegistry screenRegistry,
                                      String typeId,
                                      String displayName,
                                      String backTarget) {
        super(SIZE, messages.get("ui.familiars.release.title", Map.of(
                "name", displayName == null || displayName.isBlank() ? "-" : displayName)));
        this.messages = messages;
        this.uiService = uiService;
        this.screenRegistry = screenRegistry;
        this.typeId = typeId;
        this.displayName = displayName;
        this.backTarget = backTarget;
    }

    @Override
    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);
        if (uiService == null || typeId == null) {
            setButton(13, Material.BARRIER, messages.get("ui.familiars.unavailable"), null, Player::closeInventory);
            return;
        }
        Component warning = messages.get("ui.familiars.release.warning", Map.of(
                "name", displayName == null ? "-" : displayName));
        setButton(13, Material.PAPER, warning, List.of(), null);

        setButton(11, Material.LIME_CONCRETE, messages.get("ui.familiars.release.confirm"), null,
                p -> handleRelease(p));
        setButton(15, Material.RED_CONCRETE, messages.get("ui.common.cancel"), null, this::returnBack);
        setButton(26, Material.OAK_DOOR, messages.get("ui.common.close"), null, Player::closeInventory);
    }

    private void handleRelease(Player player) {
        FamiliarActionResult result = uiService.release(player, typeId);
        if (result.allowed()) {
            if (screenRegistry != null) {
                screenRegistry.open(player, "familiars.manager");
            } else {
                player.closeInventory();
            }
            return;
        }
        player.sendMessage(messages.get("ui.familiars.action.failed"));
        for (String reason : result.reasons()) {
            player.sendMessage(Component.text(" - " + reason));
        }
    }

    private void returnBack(Player player) {
        if (screenRegistry != null && backTarget != null && !backTarget.isBlank()) {
            screenRegistry.open(player, backTarget);
        } else {
            player.closeInventory();
        }
    }
}
