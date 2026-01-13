package ru.realite.classes.menu;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.classes.model.HudMode;
import ru.realite.classes.util.Components;
import ru.realite.ui.menu.BaseMenu;

public final class ClassSettingsMenu extends BaseMenu {

    private static final int SIZE = 9;

    private final ClassMenuManager manager;

    public ClassSettingsMenu(ClassMenuManager manager) {
        super(SIZE, Components.cSection(manager.messages().get("menu.settings.title")));
        this.manager = manager;
    }

    public void open(Player player) {
        build();
        super.open(player);
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);

        setButton(1, Material.DRAGON_BREATH,
                Components.cSection(manager.messages().get("menu.settings.bossbar")),
                lore("menu.settings.bossbar-lore"),
                p -> manager.applyHudMode(p, HudMode.BOSSBAR));
        setButton(3, Material.PAPER,
                Components.cSection(manager.messages().get("menu.settings.actionbar")),
                lore("menu.settings.actionbar-lore"),
                p -> manager.applyHudMode(p, HudMode.ACTIONBAR));
        setButton(5, Material.OAK_SIGN,
                Components.cSection(manager.messages().get("menu.settings.sidebar")),
                lore("menu.settings.sidebar-lore"),
                p -> manager.applyHudMode(p, HudMode.SIDEBAR));
        setButton(7, Material.BARRIER,
                Components.cSection(manager.messages().get("menu.settings.off")),
                lore("menu.settings.off-lore"),
                p -> manager.applyHudMode(p, HudMode.OFF));
    }

    private List<net.kyori.adventure.text.Component> lore(String key) {
        return manager.messages().getList(key)
                .stream()
                .map(Components::cSection)
                .toList();
    }
}
