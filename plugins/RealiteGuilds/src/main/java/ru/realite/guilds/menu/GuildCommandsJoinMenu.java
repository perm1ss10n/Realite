package ru.realite.guilds.menu;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsJoinMenu extends GuildMenu {

    private final List<String> invites;

    public GuildCommandsJoinMenu(GuildMenuManager manager, GuildMessages messages, List<String> invites) {
        super(manager, messages, 27, "gui.title.basics");
        this.invites = invites == null ? List.of() : List.copyOf(invites);
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (String tag : invites) {
            List<Component> lore = new ArrayList<>();
            lore.add(messages.msg("gui.lore.run"));
            setButton(slot, Material.PAPER, colored(messages.raw("gui.button.join") + " " + tag), lore,
                    player -> manager.runCommand(player, "g join " + tag));
            slot++;
            if (slot == 17) {
                break;
            }
        }

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openBasics);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
