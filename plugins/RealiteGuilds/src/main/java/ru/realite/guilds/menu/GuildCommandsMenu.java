package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.Material;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildCommandsMenu extends GuildMenu {

    public GuildCommandsMenu(GuildMenuManager manager, GuildMessages messages) {
        super(manager, messages, 27, "gui.title.root");
        build();
    }

    private void build() {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);

        setButton(10, Material.BOOK, "gui.button.basics", List.of("gui.lore.open"), manager::openBasics);
        setButton(12, Material.NAME_TAG, "gui.button.members", List.of("gui.lore.open"), manager::openMembers);
        setButton(14, Material.GRASS_BLOCK, "gui.button.territory", List.of("gui.lore.open"), manager::openTerritory);
        setButton(16, Material.GOLD_INGOT, "gui.button.economy", List.of("gui.lore.open"), manager::openEconomy);

        setButton(22, Material.BARRIER, "gui.button.close", List.of(), player -> player.closeInventory());
    }
}
