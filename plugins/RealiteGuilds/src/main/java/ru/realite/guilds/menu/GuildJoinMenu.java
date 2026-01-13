package ru.realite.guilds.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildJoinMenu extends GuildMenu {

    private static final int SIZE = 27;

    public GuildJoinMenu(GuildMenuManager manager, GuildMessages messages, List<String> invites) {
        super(manager, messages, SIZE, "gui.join.title");
        build(invites);
    }

    private void build(List<String> invites) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        int index = 10;
        for (String tag : invites) {
            Component name = messages.msg("gui.join.entry", "tag", tag);
            setButton(index, Material.OAK_DOOR, name, List.of(messages.msg("gui.join.lore")), player -> {
                manager.runCommand(player, "g join " + tag);
                player.closeInventory();
            });
            index++;
        }
        setButton(18, Material.ARROW, "gui.menu.back", List.of("gui.menu.back_lore"), manager::openRoot);
        setButton(26, Material.BARRIER, "gui.menu.close", List.of(), Player::closeInventory);
    }
}
