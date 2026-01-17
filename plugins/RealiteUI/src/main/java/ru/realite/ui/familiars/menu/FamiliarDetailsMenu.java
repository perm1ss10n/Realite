package ru.realite.ui.familiars.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.core.api.familiars.FamiliarActionResult;
import ru.realite.core.api.familiars.FamiliarDetailsData;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.familiars.FamiliarUiState;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.menu.BaseMenu;

public final class FamiliarDetailsMenu extends BaseMenu {

    private static final int SIZE = 45;

    private final MiniMessageMessages messages;
    private final FamiliarUiService uiService;
    private final UiScreenRegistry screenRegistry;
    private final String typeId;
    private final String displayName;

    public FamiliarDetailsMenu(MiniMessageMessages messages,
                               FamiliarUiService uiService,
                               UiScreenRegistry screenRegistry,
                               String typeId,
                               String displayName) {
        super(SIZE, messages.get("ui.familiars.details.title", Map.of(
                "name", displayName == null || displayName.isBlank() ? "-" : displayName)));
        this.messages = messages;
        this.uiService = uiService;
        this.screenRegistry = screenRegistry;
        this.typeId = typeId;
        this.displayName = displayName;
    }

    @Override
    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);
        if (uiService == null || typeId == null) {
            setButton(22, Material.BARRIER, messages.get("ui.familiars.unavailable"), null, Player::closeInventory);
            return;
        }
        Optional<FamiliarDetailsData> dataOptional = uiService.detailsData(player, typeId);
        if (dataOptional.isEmpty()) {
            setButton(22, Material.BARRIER, messages.get("ui.familiars.details.missing"), null, p -> openManager(p));
            return;
        }
        FamiliarDetailsData data = dataOptional.get();
        List<Component> statsLore = new ArrayList<>();
        if (data.stats().isEmpty()) {
            statsLore.add(messages.get("ui.familiars.details.stats.empty"));
        } else {
            data.stats().forEach((key, value) -> statsLore.add(messages.get("ui.familiars.details.stats.entry",
                    Map.of("stat", key, "value", String.valueOf(value)))));
        }
        setButton(11, Material.BOOK, messages.get("ui.familiars.details.stats.title"), statsLore, null);

        List<Component> progressLore = List.of(
                messages.get("ui.familiars.details.progress.level", Map.of("level", String.valueOf(data.level()))),
                messages.get("ui.familiars.details.progress.xp", Map.of(
                        "xp", String.valueOf(data.xp()),
                        "max", String.valueOf(data.xpMax()))));
        setButton(13, Material.EXPERIENCE_BOTTLE, messages.get("ui.familiars.details.progress.title"), progressLore, null);

        List<Component> talentsLore = data.talents().isEmpty()
                ? List.of(messages.get("ui.familiars.details.talents.empty"))
                : data.talents().stream()
                        .map(talent -> messages.get("ui.familiars.details.talents.entry", Map.of("talent", talent)))
                        .toList();
        setButton(15, Material.NETHER_STAR, messages.get("ui.familiars.details.talents.title"), talentsLore, null);

        if (data.inventoryEnabled()) {
            List<Component> inventoryLore = data.inventory().isEmpty()
                    ? List.of(messages.get("ui.familiars.details.inventory.empty"))
                    : data.inventory().stream()
                            .map(item -> messages.get("ui.familiars.details.inventory.entry", Map.of("item", item)))
                            .toList();
            setButton(29, Material.CHEST, messages.get("ui.familiars.details.inventory.title"), inventoryLore,
                    p -> {
                        if (!uiService.openInventory(p, data.typeId())) {
                            p.sendMessage(messages.get("ui.action.unavailable"));
                        }
                    });
        } else {
            setButton(29, Material.GRAY_DYE, messages.get("ui.familiars.details.inventory.title"),
                    List.of(messages.get("ui.action.unavailable")), null);
        }

        renderActions(player, data);
    }

    private void renderActions(Player player, FamiliarDetailsData data) {
        boolean summoned = data.state() == FamiliarUiState.SUMMONED;
        if (summoned) {
            setButton(38, Material.BARRIER, messages.get("ui.familiars.manager.action.dismiss"), null,
                    p -> handleResult(p, uiService.dismiss(p, data.typeId())));
        } else {
            setButton(38, Material.LIME_DYE, messages.get("ui.familiars.manager.action.summon"), null,
                    p -> handleResult(p, uiService.summon(p, data.typeId())));
        }

        if (summoned) {
            setButton(40, Material.NETHER_STAR, messages.get("ui.familiars.manager.action.set_active"), null,
                    p -> handleResult(p, uiService.setActive(p, data.typeId())));
        } else {
            setButton(40, Material.GRAY_DYE, messages.get("ui.familiars.manager.action.set_active"),
                    List.of(messages.get("ui.action.unavailable")), null);
        }

        setButton(42, Material.NAME_TAG, messages.get("ui.familiars.manager.action.rename"),
                List.of(messages.get("ui.common.coming_soon")), null);
        setButton(43, Material.TNT, messages.get("ui.familiars.manager.action.release"), null,
                p -> {
                    if (screenRegistry != null) {
                        screenRegistry.open(p, "familiars.release:type=" + data.typeId() + ";back=details");
                    }
                });
        setButton(44, Material.ARROW, messages.get("ui.common.back"), null, this::openManager);
        setButton(36, Material.OAK_DOOR, messages.get("ui.common.close"), null, Player::closeInventory);
    }

    private void openManager(Player player) {
        if (screenRegistry != null) {
            screenRegistry.open(player, "familiars.manager");
        } else {
            player.closeInventory();
        }
    }

    private void handleResult(Player player, FamiliarActionResult result) {
        if (result.allowed()) {
            if (screenRegistry != null) {
                screenRegistry.open(player, "familiars.details:" + typeId);
            }
            return;
        }
        player.sendMessage(messages.get("ui.familiars.action.failed"));
        for (String reason : result.reasons()) {
            player.sendMessage(Component.text(" - " + reason));
        }
    }
}
