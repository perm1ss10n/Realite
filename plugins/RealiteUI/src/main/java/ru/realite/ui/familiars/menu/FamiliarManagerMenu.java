package ru.realite.ui.familiars.menu;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.core.api.familiars.FamiliarActionResult;
import ru.realite.core.api.familiars.FamiliarManagerData;
import ru.realite.core.api.familiars.FamiliarSummary;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.familiars.FamiliarUiState;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.menu.BaseMenu;

public final class FamiliarManagerMenu extends BaseMenu {

    private static final int SIZE = 54;
    private static final int[] CARD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final MiniMessageMessages messages;
    private final FamiliarUiService uiService;
    private final UiScreenRegistry screenRegistry;
    private final String selectedTypeId;

    public FamiliarManagerMenu(MiniMessageMessages messages,
                               FamiliarUiService uiService,
                               UiScreenRegistry screenRegistry,
                               String selectedTypeId) {
        super(SIZE, messages.get("ui.familiars.manager.title"));
        this.messages = messages;
        this.uiService = uiService;
        this.screenRegistry = screenRegistry;
        this.selectedTypeId = selectedTypeId;
    }

    @Override
    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);
        if (uiService == null) {
            setButton(22, Material.BARRIER, messages.get("ui.familiars.unavailable"), null, Player::closeInventory);
            return;
        }
        Optional<FamiliarManagerData> dataOptional = uiService.managerData(player);
        if (dataOptional.isEmpty()) {
            setButton(22, Material.BARRIER, messages.get("ui.familiars.empty"), null, Player::closeInventory);
            return;
        }
        FamiliarManagerData data = dataOptional.get();
        List<FamiliarSummary> familiars = data.familiars();
        if (familiars.isEmpty()) {
            setButton(22, Material.BARRIER, messages.get("ui.familiars.empty"), null, Player::closeInventory);
            return;
        }
        String effectiveSelection = resolveSelection(data, familiars);

        int slotIndex = 0;
        for (FamiliarSummary summary : familiars) {
            if (slotIndex >= CARD_SLOTS.length) {
                break;
            }
            String stateLabel = stateLabel(summary.state());
            Component name = messages.get("ui.familiars.manager.card.name", Map.of(
                    "name", summary.name(),
                    "level", String.valueOf(summary.level())));
            List<Component> lore = new java.util.ArrayList<>();
            lore.add(messages.get("ui.familiars.manager.card.mob", Map.of("mob", summary.mobType())));
            lore.add(messages.get("ui.familiars.manager.card.role", Map.of("role", summary.role())));
            lore.add(messages.get("ui.familiars.manager.card.state", Map.of("state", stateLabel)));
            if (summary.typeId().equalsIgnoreCase(data.activeTypeId().orElse(""))) {
                lore.add(messages.get("ui.familiars.manager.card.active"));
            }
            Material icon = summary.state() == FamiliarUiState.SUMMONED ? Material.LIME_DYE : Material.GRAY_DYE;
            if (summary.typeId().equalsIgnoreCase(effectiveSelection)) {
                icon = Material.EMERALD;
            }
            String typeId = summary.typeId();
            setButton(CARD_SLOTS[slotIndex++], icon, name, lore, p -> {
                if (screenRegistry != null) {
                    screenRegistry.open(p, "familiars.manager:select=" + typeId);
                }
            });
        }

        setButton(45, Material.OAK_DOOR, messages.get("ui.common.close"), null, Player::closeInventory);
        renderActions(player, data, effectiveSelection);
    }

    private void renderActions(Player player, FamiliarManagerData data, String selection) {
        FamiliarSummary summary = data.familiars().stream()
                .filter(item -> item.typeId().equalsIgnoreCase(selection))
                .findFirst()
                .orElse(null);
        if (summary == null) {
            renderDisabledActions();
            return;
        }
        boolean summoned = summary.state() == FamiliarUiState.SUMMONED;
        if (summoned) {
            setButton(47, Material.BARRIER, messages.get("ui.familiars.manager.action.dismiss"), null,
                    p -> handleResult(p, uiService.dismiss(p, summary.typeId()), selection));
        } else {
            setButton(47, Material.LIME_DYE, messages.get("ui.familiars.manager.action.summon"), null,
                    p -> handleResult(p, uiService.summon(p, summary.typeId()), selection));
        }

        if (summoned) {
            setButton(49, Material.NETHER_STAR, messages.get("ui.familiars.manager.action.set_active"), null,
                    p -> handleResult(p, uiService.setActive(p, summary.typeId()), selection));
        } else {
            setButton(49, Material.GRAY_DYE, messages.get("ui.familiars.manager.action.set_active"),
                    List.of(messages.get("ui.action.unavailable")), null);
        }

        setButton(51, Material.BOOK, messages.get("ui.familiars.manager.action.details"), null, p -> {
            if (screenRegistry != null) {
                screenRegistry.open(p, "familiars.details:" + summary.typeId());
            }
        });
        setButton(53, Material.NAME_TAG, messages.get("ui.familiars.manager.action.rename"),
                List.of(messages.get("ui.common.coming_soon")), null);
    }

    private void renderDisabledActions() {
        setButton(47, Material.GRAY_DYE, messages.get("ui.familiars.manager.action.summon"),
                List.of(messages.get("ui.action.unavailable")), null);
        setButton(49, Material.GRAY_DYE, messages.get("ui.familiars.manager.action.set_active"),
                List.of(messages.get("ui.action.unavailable")), null);
        setButton(51, Material.GRAY_DYE, messages.get("ui.familiars.manager.action.details"),
                List.of(messages.get("ui.action.unavailable")), null);
        setButton(53, Material.NAME_TAG, messages.get("ui.familiars.manager.action.rename"),
                List.of(messages.get("ui.common.coming_soon")), null);
    }

    private void handleResult(Player player, FamiliarActionResult result, String selection) {
        if (result.allowed()) {
            if (screenRegistry != null) {
                screenRegistry.open(player, "familiars.manager:select=" + selection);
            }
            return;
        }
        player.sendMessage(messages.get("ui.familiars.action.failed"));
        for (String reason : result.reasons()) {
            player.sendMessage(Component.text(" - " + reason));
        }
    }

    private String resolveSelection(FamiliarManagerData data, List<FamiliarSummary> familiars) {
        if (selectedTypeId != null) {
            for (FamiliarSummary summary : familiars) {
                if (summary.typeId().equalsIgnoreCase(selectedTypeId)) {
                    return summary.typeId();
                }
            }
        }
        if (data.activeTypeId().isPresent()) {
            return data.activeTypeId().get();
        }
        return familiars.get(0).typeId();
    }

    private String stateLabel(FamiliarUiState state) {
        return messages.rawOr("ui.familiars.state." + state.name().toLowerCase(), state.name().toLowerCase());
    }
}
