package ru.realite.magic.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicMenuListener implements Listener {

    private static final String PERMISSION_SELECT = "realite.magic.spell.select";
    private static final String PERMISSION_ADMIN = "realite.magic.admin";

    private final SpellSelectMenu menu;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;

    public MagicMenuListener(SpellSelectMenu menu,
                             SpellRegistry spellRegistry,
                             PlayerSpellService playerSpellService,
                             MagicMessages messages) {
        this.menu = menu;
        this.spellRegistry = spellRegistry;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

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

        SpellDefinition spell = spellRegistry.get(spellId);
        if (spell == null) {
            return;
        }

        if (!hasSelectPermission(player)) {
            player.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return;
        }

        if (!playerSpellService.hasSpell(player.getUniqueId(), spell.id())) {
            player.sendMessage(messages.msg("magic.spell.select.not_learned",
                    "spell", messages.raw(spell.nameKey())));
            return;
        }

        playerSpellService.select(player.getUniqueId(), spell.id());
        if (menu.shouldCloseOnSelect()) {
            player.closeInventory();
            return;
        }
        menu.open(player);
    }

    private boolean hasSelectPermission(Player player) {
        return player.hasPermission(PERMISSION_SELECT) || player.hasPermission(PERMISSION_ADMIN);
    }
}
