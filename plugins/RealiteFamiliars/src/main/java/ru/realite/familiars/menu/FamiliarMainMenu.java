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

public final class FamiliarMainMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final FamiliarMenuManager manager;

    public FamiliarMainMenu(FamiliarMenuManager manager) {
        super(SIZE, title(manager));
        this.manager = manager;
    }

    @Override
    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        FamiliarService service = manager.service();
        List<FamiliarInstance> familiars = service.getFamiliars(player.getUniqueId());
        if (familiars.isEmpty()) {
            setButton(13, Material.BARRIER,
                    manager.messages().get("menu.empty"),
                    null,
                    Player::closeInventory);
            return;
        }

        int slot = 10;
        for (FamiliarInstance instance : familiars) {
            if (slot >= 17) {
                break;
            }
            FamiliarType type = service.getType(instance.typeId()).orElse(null);
            String role = type != null ? type.role() : "-";
            String state = instance.state() == FamiliarState.SUMMONED
                    ? manager.messages().raw("menu.state.summoned")
                    : manager.messages().raw("menu.state.tamed");
            if (state == null || state.isBlank()) {
                state = instance.state() == FamiliarState.SUMMONED ? "summoned" : "tamed";
            }
            int xp = clampPercent(instance.xp());
            List<Component> lore = new ArrayList<>();
            lore.add(manager.messages().get("menu.familiar.lore.role", Map.of("role", role)));
            lore.add(manager.messages().get("menu.familiar.lore.level", Map.of(
                    "level", String.valueOf(instance.level()),
                    "xp", String.valueOf(xp))));
            lore.add(manager.messages().get("menu.familiar.lore.state", Map.of("state", state)));

            Material icon = instance.state() == FamiliarState.SUMMONED ? Material.LIME_DYE : Material.GRAY_DYE;
            Component name = manager.messages().get("menu.familiar.name", Map.of(
                    "type", instance.typeId(),
                    "level", String.valueOf(instance.level())));

            String typeId = instance.typeId();
            setButton(slot++, icon, name, lore, p -> manager.openControl(p, typeId));
        }

        setButton(22, Material.OAK_DOOR,
                manager.messages().get("menu.close"),
                null,
                Player::closeInventory);
    }

    private static Component title(FamiliarMenuManager manager) {
        return manager.messages().get("menu.title");
    }

    private int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
