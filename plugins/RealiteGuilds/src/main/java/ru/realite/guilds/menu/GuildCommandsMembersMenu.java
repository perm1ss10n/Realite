package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsMembersMenu extends GuildMenu {

    public GuildCommandsMembersMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.members");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(11, Material.PAPER, "gui.button.invite", List.of("gui.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.INVITE));
        setButton(13, Material.NAME_TAG, "gui.button.ranks", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g ranks"));
        setButton(15, Material.ANVIL, "gui.button.setrank", List.of("gui.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.SETRANK));

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openRoot);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
