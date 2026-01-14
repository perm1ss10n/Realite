package ru.realite.classes.menu;

import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;
import ru.realite.classes.util.Components;
import ru.realite.ui.menu.BaseMenu;

public final class ClassConfirmMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final ClassMenuManager manager;
    private final ClassId classId;

    public ClassConfirmMenu(ClassMenuManager manager, ClassId classId) {
        super(SIZE, title(manager, classId));
        this.manager = manager;
        this.classId = classId;
    }

    public void open(Player player) {
        build();
        super.open(player);
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        var def = manager.classConfig().get(classId);
        var loreDef = manager.classLore() != null ? manager.classLore().get(classId) : null;

        Material icon = loreDef != null ? loreDef.icon : null;
        if (icon == null && def != null) {
            icon = def.icon;
        }

        setButton(13,
                icon != null ? icon : Material.PAPER,
                Components.c(displayName(def, loreDef)),
                null,
                null);

        setButton(21, Material.LIME_WOOL,
                Components.cSection(manager.messages().get("ui.common.confirm")),
                null,
                p -> manager.assignClass(p, classId));
        setButton(23, Material.ARROW,
                Components.cSection(manager.messages().get("ui.common.back")),
                null,
                p -> manager.openDetails(p, classId));
    }

    private static Component title(ClassMenuManager manager, ClassId classId) {
        var def = manager.classConfig().get(classId);
        var loreDef = manager.classLore() != null ? manager.classLore().get(classId) : null;
        return Components.cSection(manager.messages().format(
                "ui.classes.confirm.title",
                Map.of("class", displayName(def, loreDef))));
    }

    private static String displayName(ClassConfigRepository.ClassDef def,
                                      ClassLoreRepository.ClassLoreDef loreDef) {
        String displayName = loreDef != null ? loreDef.displayName : null;
        if (displayName == null || displayName.isBlank()) {
            displayName = def != null ? def.name : null;
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = "-";
        }
        return displayName;
    }
}
