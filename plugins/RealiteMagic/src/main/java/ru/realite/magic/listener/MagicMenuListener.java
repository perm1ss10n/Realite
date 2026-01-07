package ru.realite.magic.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;

public final class MagicMenuListener implements Listener {

    private final MagicService magicService;
    private final MagicMessages messages;

    public MagicMenuListener(MagicService magicService, MagicMessages messages) {
        this.magicService = magicService;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof SpellSelectMenu menu)) {
            return;
        }
        event.setCancelled(true);

        if (event.getClickedInventory() == null) {
            return;
        }
        if (event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }

        if (menu.isCloseButton(event.getCurrentItem())) {
            player.closeInventory();
            return;
        }

        String spellId = menu.extractSpellId(event.getCurrentItem());
        if (spellId == null) {
            return;
        }

        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            return;
        }

        if (!magicService.meetsRequirements(player, spell)) {
            String reason = menu.requirementReason(spell);
            if (reason != null && !reason.isBlank()) {
                player.sendMessage(messages.msg("magic.error.cannot_select_spell", "reason", reason));
            }
            return;
        }

        magicService.setActiveSpell(player, spellId);
        player.sendMessage(messages.msg("magic.spell.selected",
                "name", messages.raw(spell.nameKey())));
        if (menu.shouldCloseOnSelect()) {
            player.closeInventory();
        }
    }
}
