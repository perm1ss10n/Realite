package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsEconomyMenu extends GuildMenu {

    public GuildCommandsEconomyMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.economy");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(10, Material.SUNFLOWER, "gui.button.salary", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g salary"));
        setButton(12, Material.NETHER_STAR, "gui.button.upgrades", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g upgrades"));
        setButton(14, Material.EMERALD, "gui.button.upgrade_buy", List.of("gui.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.UPGRADE_BUY));
        setButton(16, Material.OAK_SIGN, "gui.button.toggle", List.of("gui.lore.run"),
                player -> manager.runCommand(player, "g toggle"));

        setButton(18, Material.ARROW, "gui.button.back", List.of("gui.lore.open"), manager::openRoot);
        setButton(26, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
