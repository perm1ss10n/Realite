package ru.realite.magic.ui.screen;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.ui.menu.MagicSpellDetailsMenu;

public final class MagicSpellDetailsScreen implements UiScreen {

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final UiScreenRegistry screenRegistry;

    public MagicSpellDetailsScreen(MagicService magicService,
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
        return "magic.spell.details";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            player.sendMessage(messages.msg("magic.spell.unknown", "spell", ""));
            return;
        }
        SpellDefinition spell = spellRegistry.get(payload.trim());
        if (spell == null) {
            player.sendMessage(messages.msg("magic.spell.unknown", "spell", payload));
            return;
        }
        new MagicSpellDetailsMenu(magicService, playerSpellService, messages, screenRegistry, spell).open(player);
    }
}
