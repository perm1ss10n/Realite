package ru.realite.guilds.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildJoinMenu extends GuildMenu {

    private static final int SIZE = 27;

    public GuildJoinMenu(GuildMenuManager manager, GuildMessages messages, List<String> invites) {
        super(manager, messages, SIZE, "ui.guild.join.title");
        build(invites);
    }

    private void build(List<String> invites) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        int index = 10;
        for (String tag : invites) {
            Component name = messages.msg("ui.guild.join.entry", "tag", tag);
            setButton(index, Material.OAK_DOOR, name, List.of(messages.msg("ui.guild.join.lore")), player -> {
                manager.runCommand(player, "g join " + tag);
                player.closeInventory();
            });
            index++;
        }
        setButton(18, Material.ARROW, "ui.common.back", List.of("ui.guild.menu.back_lore"), manager::openRoot);
        setButton(26, Material.BARRIER, "ui.common.close", List.of(), Player::closeInventory);
    }
}
