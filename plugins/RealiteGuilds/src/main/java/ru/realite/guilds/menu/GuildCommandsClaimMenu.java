package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsClaimMenu extends GuildMenu {

    public GuildCommandsClaimMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.claim");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(11, Material.WOODEN_AXE, "gui.button.claim_pos1", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g claim pos1"));
        setButton(13, Material.STONE_AXE, "gui.button.claim_pos2", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g claim pos2"));
        setButton(15, Material.LIME_CONCRETE, "gui.button.claim_apply", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g claim apply"));
        setButton(21, Material.RED_CONCRETE, "gui.button.claim_clear", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g claim clear"));

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openTerritory);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
