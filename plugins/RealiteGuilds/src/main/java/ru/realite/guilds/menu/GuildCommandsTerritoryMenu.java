package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsTerritoryMenu extends GuildMenu {

    public GuildCommandsTerritoryMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.territory");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(10, Material.OAK_FENCE, "gui.button.claim", List.of("gui.lore.open"), manager::openClaim);
        setButton(12, Material.ENDER_PEARL, "gui.button.home", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g home"));
        setButton(14, Material.RED_BED, "gui.button.sethome", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g sethome"));
        setButton(16, Material.ENDER_EYE, "gui.button.tp", List.of("gui.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.TP));

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openRoot);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
