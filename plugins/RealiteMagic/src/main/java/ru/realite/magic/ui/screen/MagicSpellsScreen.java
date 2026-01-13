package ru.realite.magic.ui.screen;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiPaginationService;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.ui.menu.MagicSpellbookMenu;

public final class MagicSpellsScreen implements UiScreen {

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiPaginationService paginationService;
    private final UiScreenRegistry screenRegistry;

    public MagicSpellsScreen(MagicService magicService,
                             SpellRegistry spellRegistry,
                             PlayerSpellService playerSpellService,
                             MagicMessages messages,
                             UiPaginationService paginationService,
                             UiScreenRegistry screenRegistry) {
        this.magicService = magicService;
        this.spellRegistry = spellRegistry;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
        this.paginationService = paginationService;
        this.screenRegistry = screenRegistry;
    }

    @Override
    public String id() {
        return "magic.spells";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        Integer selectionSlot = parseSelectionSlot(payload);
        new MagicSpellbookMenu(magicService, spellRegistry, playerSpellService, messages,
                paginationService, screenRegistry, 0, selectionSlot).open(player);
    }

    private Integer parseSelectionSlot(@Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String raw = payload.trim();
        if (raw.startsWith("slot=")) {
            raw = raw.substring("slot=".length());
        } else if (raw.startsWith("slot:")) {
            raw = raw.substring("slot:".length());
        }
        try {
            int slot = Integer.parseInt(raw);
            if (slot <= 0 || slot > magicService.slotCount()) {
                return null;
            }
            return slot;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
