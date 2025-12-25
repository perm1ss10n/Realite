package ru.realite.classes.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class MenuListener implements Listener {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final EvolutionService evolutionService;
    private final Messages messages;

    public MenuListener(ClassService classService,
                        ClassConfigRepository classConfig,
                        EvolutionService evolutionService,
                        Messages messages) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.evolutionService = evolutionService;
        this.messages = messages;
    }

    private String requirementsText(ClassConfigRepository.ClassDef def) {
        if (def.requiresMastered == null || def.requiresMastered.isEmpty()) return "-";

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (var reqId : def.requiresMastered) {
            var reqDef = classConfig.get(reqId);
            String nice = (reqDef != null ? reqDef.name : reqId.name());

            if (!first) sb.append(", ");
            first = false;
            sb.append(nice);
        }

        return sb.toString();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof ClassSelectMenu menu)) return;

        e.setCancelled(true);

        if (e.getClickedInventory() == null) return;
        if (e.getClickedInventory().equals(player.getInventory())) return;

        ClassId id = menu.extractClassId(e.getCurrentItem());
        if (id == null) return;

        var prof = classService.getProfile(player);
        if (prof == null) return;

        // если класс уже выбран — это "смена", применяем правила смены
        if (prof.hasClass() && !evolutionService.canChangeClass(player, prof)) {
            player.sendMessage(messages.get("cant-change"));
            player.closeInventory();
            return;
        }

        // скрытый класс: виден, но выбрать нельзя
        var def = classConfig.get(id);
        if (def != null && def.hidden) {
            boolean unlocked = true;
            for (var req : def.requiresMastered) {
                if (!prof.hasMastered(req)) {
                    unlocked = false;
                    break;
                }
            }

            if (!unlocked) {
                player.closeInventory();
                player.sendMessage(messages.get("class-locked"));
                player.sendMessage(messages.format("class-locked-requirements", Map.of(
                        "req", requirementsText(def)
                )));
                return;
            }
        }

        classService.assignClass(player, id);

        // снимаем "стартовый", раз он выбрал класс вручную
        prof.setStarterClass(false);
        classService.save(prof);

        String niceName = (def != null ? def.name : id.name());
        player.sendMessage(messages.format("chosen", Map.of("class", niceName)));

        player.closeInventory();
    }
}
