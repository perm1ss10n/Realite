package ru.realite.magic.ui.screen;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.ui.menu.MagicLoadoutMenu;

public final class MagicLoadoutScreen implements UiScreen {

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiScreenRegistry screenRegistry;

    public MagicLoadoutScreen(MagicService magicService,
                              SpellRegistry spellRegistry,
                              PlayerSpellService playerSpellService,
                              MagicMessages messages,
                              UiScreenRegistry screenRegistry) {
        this.magicService = magicService;
        this.spellRegistry = spellRegistry;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
        this.screenRegistry = screenRegistry;
    }

    @Override
    public String id() {
        return "magic.loadout";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        if (magicService.slotCount() <= 0) {
            screenRegistry.open(player, "magic.spells");
            return;
        }
        new MagicLoadoutMenu(magicService, spellRegistry, playerSpellService, messages, screenRegistry)
                .open(player);
    }
}
