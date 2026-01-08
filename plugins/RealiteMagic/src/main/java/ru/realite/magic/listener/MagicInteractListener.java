package ru.realite.magic.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicInteractListener implements Listener {

    private static final String PERMISSION_USE = "realite.magic.use";

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final SpellRegistry spellRegistry;
    private final MagicMessages messages;

    public MagicInteractListener(MagicService magicService,
                                 PlayerSpellService playerSpellService,
                                 SpellRegistry spellRegistry,
                                 MagicMessages messages) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.spellRegistry = spellRegistry;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.getPlayer().hasPermission(PERMISSION_USE)) {
            event.getPlayer().sendMessage(messages.msg("magic.command.errors.no_permission"));
            return;
        }

        var playerId = event.getPlayer().getUniqueId();
        var selectedSpellId = playerSpellService.getSelected(playerId).orElse(null);
        if (selectedSpellId == null) {
            event.getPlayer().sendMessage(messages.msg("magic.cast.no_selected"));
            return;
        }
        SpellDefinition spell = spellRegistry.find(selectedSpellId).orElse(null);
        if (spell == null) {
            playerSpellService.clearSelected(playerId);
            event.getPlayer().sendMessage(messages.msg("magic.spell.unknown", "spell", selectedSpellId));
            return;
        }

        if (!magicService.hasRequiredFocus(event.getPlayer())) {
            event.getPlayer().sendMessage(messages.msg("magic.error.need_focus"));
            return;
        }

        magicService.cast(event.getPlayer(), spell);
    }
}
