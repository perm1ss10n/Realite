package ru.realite.classes.menu;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.HiddenClassGateResult;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;
import ru.realite.classes.util.Components;
import ru.realite.ui.menu.BaseMenu;

public final class ClassMainMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final ClassMenuManager manager;

    public ClassMainMenu(ClassMenuManager manager) {
        super(SIZE, title(manager));
        this.manager = manager;
    }

    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        ClassConfigRepository classConfig = manager.classConfig();
        ClassLoreRepository classLore = manager.classLore();

        ClassId[] ids = ClassId.values();
        int index = 0;
        int rows = SIZE / 9;

        for (int row = 0; row < rows && index < ids.length; row++) {
            int rowStart = row * 9;
            int remaining = ids.length - index;
            int countInRow = Math.min(9, remaining);
            int startSlot = rowStart + (9 - countInRow) / 2;

            for (int i = 0; i < countInRow; i++) {
                ClassId id = ids[index++];
                var def = classConfig.get(id);
                var loreDef = classLore != null ? classLore.get(id) : null;

                boolean hidden = (def != null && def.hidden) || (loreDef != null && loreDef.hiddenEnabled);
                HiddenClassGateResult gateResult = hidden && manager.hiddenClassGate() != null
                        ? manager.hiddenClassGate().check(player, id)
                        : null;
                boolean hiddenLocked = hidden && gateResult != null && !gateResult.available();

                Material icon = loreDef != null ? loreDef.icon : null;
                if (icon == null && def != null) {
                    icon = def.icon;
                }

                Material material = hiddenLocked
                        ? (loreDef != null && loreDef.lockedIcon != null ? loreDef.lockedIcon : Material.BARRIER)
                        : (icon != null ? icon : Material.PAPER);

                Component name = hiddenLocked
                        ? Components.c(defaultLockedName(loreDef))
                        : Components.c(displayName(def, loreDef, id));

                List<Component> lore = new ArrayList<>();
                if (hiddenLocked) {
                    List<String> lockedLore = loreDef != null ? loreDef.lockedLore : List.of();
                    if (lockedLore.isEmpty()) {
                        lore.add(Components.c("&7..."));
                    } else {
                        for (String line : lockedLore) {
                            lore.add(Components.c(line));
                        }
                    }
                } else {
                    List<String> lines = loreDef != null ? loreDef.lore : List.of();
                    if ((lines == null || lines.isEmpty()) && def != null) {
                        lines = def.lore;
                    }
                    if (lines != null) {
                        for (String line : lines) {
                            lore.add(Components.c(line));
                        }
                    }
                }

                setButton(startSlot + i, material, name, lore,
                        hiddenLocked ? null : p -> manager.openDetails(p, id));
            }
        }

        setButton(22, Material.ENCHANTED_BOOK,
                Components.cSection(manager.messages().get("ui.classes.main.progress")),
                componentsFromMessages("ui.classes.main.progress-lore"),
                manager::openProgress);
        setButton(26, Material.OAK_DOOR,
                Components.cSection(manager.messages().get("ui.common.close")),
                null,
                Player::closeInventory);
    }

    private List<Component> componentsFromMessages(String key) {
        List<String> lines = manager.messages().getList(key);
        List<Component> out = new ArrayList<>();
        for (String line : lines) {
            out.add(Components.cSection(line));
        }
        return out;
    }

    private static Component title(ClassMenuManager manager) {
        return Components.cSection(manager.messages().get("ui.classes.main.title"));
    }

    private static String defaultLockedName(ClassLoreRepository.ClassLoreDef loreDef) {
        String lockedName = loreDef != null ? loreDef.lockedName : null;
        if (lockedName == null || lockedName.isBlank()) {
            lockedName = "&8???";
        }
        return lockedName;
    }

    private static String displayName(ClassConfigRepository.ClassDef def,
                                      ClassLoreRepository.ClassLoreDef loreDef,
                                      ClassId id) {
        String displayName = loreDef != null ? loreDef.displayName : null;
        if (displayName == null || displayName.isBlank()) {
            displayName = def != null ? def.name : id.name();
        }
        return displayName;
    }
}
