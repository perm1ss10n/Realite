package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsBasicsMenu extends GuildMenu {

    public GuildCommandsBasicsMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.basics");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(10, Material.ANVIL, "gui.button.create", List.of("gui.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.CREATE));
        setButton(12, Material.BOOK, "gui.button.info", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g info"));
        setButton(14, Material.OAK_DOOR, "gui.button.join", List.of("gui.lore.run"),
                manager::handleJoin);
        setButton(16, Material.IRON_DOOR, "gui.button.leave", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g leave"));
        setButton(22, Material.TNT, "gui.button.disband", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g disband"));

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openRoot);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
