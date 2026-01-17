package ru.realite.familiars.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;
import ru.realite.familiars.model.FamiliarType;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.ui.menu.BaseMenu;

public final class FamiliarControlMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final FamiliarMenuManager manager;
    private final String typeId;

    public FamiliarControlMenu(FamiliarMenuManager manager, String typeId) {
        super(SIZE, title(manager, typeId));
        this.manager = manager;
        this.typeId = typeId;
    }

    @Override
    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);
        FamiliarService service = manager.service();
        FamiliarInstance instance = service.getFamiliars(player.getUniqueId()).stream()
                .filter(familiar -> familiar.typeId().equalsIgnoreCase(typeId))
                .findFirst()
                .orElse(null);
        if (instance == null) {
            setButton(13, Material.BARRIER, manager.messages().get("menu.familiar.missing"), null, p -> manager.openMain(p));
            return;
        }

        FamiliarType type = service.getType(instance.typeId()).orElse(null);
        String role = type != null ? type.role() : "-";
        String state = instance.state() == FamiliarState.SUMMONED
                ? manager.messages().raw("menu.state.summoned")
                : manager.messages().raw("menu.state.tamed");
        if (state == null || state.isBlank()) {
            state = instance.state() == FamiliarState.SUMMONED ? "summoned" : "tamed";
        }

        List<Component> infoLore = new ArrayList<>();
        infoLore.add(manager.messages().get("menu.familiar.lore.role", Map.of("role", role)));
        infoLore.add(manager.messages().get("menu.familiar.lore.state", Map.of("state", state)));
        infoLore.add(manager.messages().get("menu.familiar.lore.level", Map.of(
                "level", String.valueOf(instance.level()),
                "xp", String.valueOf(clampPercent(instance.xp())))));
        setButton(13, Material.BOOK,
                manager.messages().get("menu.familiar.name", Map.of(
                        "type", instance.typeId(),
                        "level", String.valueOf(instance.level()))),
                infoLore,
                null);

        boolean summoned = instance.state() == FamiliarState.SUMMONED;
        if (summoned) {
            setButton(10, Material.BARRIER,
                    manager.messages().get("menu.action.dismiss"),
                    null,
                    p -> manager.runCommand(p, "familiar dismiss " + typeId, () -> manager.openControl(p, typeId)));
        } else {
            setButton(10, Material.LIME_DYE,
                    manager.messages().get("menu.action.summon"),
                    null,
                    p -> manager.runCommand(p, "familiar summon " + typeId, () -> manager.openControl(p, typeId)));
        }

        if (summoned) {
            setButton(12, Material.LEAD,
                    manager.messages().get("menu.action.follow"),
                    null,
                    p -> manager.runCommand(p, "familiar follow " + typeId, () -> manager.openControl(p, typeId)));
            setButton(14, Material.REDSTONE_TORCH,
                    manager.messages().get("menu.action.stay"),
                    null,
                    p -> manager.runCommand(p, "familiar stay " + typeId, () -> manager.openControl(p, typeId)));
        } else {
            setButton(12, Material.GRAY_DYE,
                    manager.messages().get("menu.action.follow"),
                    List.of(manager.messages().get("menu.action.unavailable")),
                    null);
            setButton(14, Material.GRAY_DYE,
                    manager.messages().get("menu.action.stay"),
                    List.of(manager.messages().get("menu.action.unavailable")),
                    null);
        }

        setButton(16, Material.NAME_TAG,
                manager.messages().get("menu.action.rename"),
                List.of(manager.messages().get("menu.action.rename_soon")),
                null);

        setButton(22, Material.ARROW,
                manager.messages().get("menu.action.back"),
                null,
                manager::openMain);
    }

    private static Component title(FamiliarMenuManager manager, String typeId) {
        return manager.messages().get("menu.familiar.title", Map.of("type", typeId));
    }

    private int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
