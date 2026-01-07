package ru.realite.magic.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;

public final class MagicMenuListener implements Listener {

    private static final String PERMISSION_SELECT = "realite.magic.spell.select";
    private static final String PERMISSION_ADMIN = "realite.magic.admin";

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

        SpellSelectMenu menu = magicService.spellSelectMenu();
        boolean menuItem = menu.isMenuItem(event.getCurrentItem());
        if (!menuItem && !menu.isMenuTitle(event.getView().title())) {
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

        if (!hasSelectPermission(player)) {
            player.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return;
        }

        if (!magicService.meetsRequirements(player, spell)) {
            String reason = menu.requirementReason(spell);
            if (reason == null) {
                reason = "";
            }
            player.sendMessage(messages.msg("magic.menu.spell_select.locked", "reason", reason));
            return;
        }

        magicService.setSelectedSpell(player, spellId);
        player.sendMessage(messages.msg("magic.menu.spell_select.selected",
                "spell", messages.raw(spell.nameKey())));
        if (menu.shouldCloseOnSelect()) {
            player.closeInventory();
        }
    }

    private boolean hasSelectPermission(Player player) {
        return player.hasPermission(PERMISSION_SELECT) || player.hasPermission(PERMISSION_ADMIN);
    }
}
