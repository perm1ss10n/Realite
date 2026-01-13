package ru.realite.classes.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.EvolutionDef;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;
import ru.realite.classes.util.Components;
import ru.realite.ui.menu.BaseMenu;

public final class ClassDetailsMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final ClassMenuManager manager;
    private final ClassId classId;

    public ClassDetailsMenu(ClassMenuManager manager, ClassId classId) {
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
        var classConfig = manager.classConfig();
        var classLore = manager.classLore();
        var def = classConfig.get(classId);
        var loreDef = classLore != null ? classLore.get(classId) : null;

        Material icon = loreDef != null ? loreDef.icon : null;
        if (icon == null && def != null) {
            icon = def.icon;
        }

        List<Component> lore = new ArrayList<>();
        List<String> description = loreDef != null ? loreDef.lore : List.of();
        if ((description == null || description.isEmpty()) && def != null) {
            description = def.lore;
        }
        if (description != null) {
            for (String line : description) {
                lore.add(Components.c(line));
            }
        }

        if (def != null && def.effects != null && !def.effects.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Components.cSection(manager.messages().get("ui.classes.details.effects-title")));
            for (String effect : def.effects) {
                lore.add(Components.c(effect));
            }
        }

        lore.add(Component.empty());
        lore.add(Components.cSection(manager.messages().get("ui.classes.details.evolutions-title")));
        List<EvolutionDef> evolutions = def != null ? def.evolutions : List.of();
        if (evolutions == null || evolutions.isEmpty()) {
            lore.add(Components.cSection(manager.messages().get("ui.classes.details.evolutions-empty")));
        } else {
            for (EvolutionDef evolution : evolutions) {
                lore.add(Components.cSection(manager.messages().format(
                        "ui.classes.details.evolution-entry",
                        Map.of(
                                "evolution", evolution.title,
                                "level", String.valueOf(evolution.requiredLevel)))));
            }
        }

        setButton(13,
                icon != null ? icon : Material.PAPER,
                Components.c(displayName(def, loreDef)),
                lore,
                null);

        setButton(21, Material.LIME_DYE,
                Components.cSection(manager.messages().get("ui.classes.details.choose")),
                null,
                p -> manager.openConfirm(p, classId));
        setButton(23, Material.ARROW,
                Components.cSection(manager.messages().get("ui.common.back")),
                null,
                manager::openMain);
    }

    private static Component title(ClassMenuManager manager, ClassId classId) {
        var def = manager.classConfig().get(classId);
        var loreDef = manager.classLore() != null ? manager.classLore().get(classId) : null;
        String name = displayName(def, loreDef);
        return Components.cSection(manager.messages().format(
                "ui.classes.details.title",
                Map.of("class", name)));
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
