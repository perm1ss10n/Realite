package ru.realite.classes.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class MenuListener implements Listener {

    private final ClassService classService;
    private final EvolutionService evolutionService;
    private final Messages messages;

    public MenuListener(ClassService classService, EvolutionService evolutionService, Messages messages) {
        this.classService = classService;
        this.evolutionService = evolutionService;
        this.messages = messages;
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

        // Если класс уже выбран — это именно "смена", значит применяем правила смены
        if (prof.hasClass() && !evolutionService.canChangeClass(player, prof)) {
            player.sendMessage(messages.get("cant-change"));
            player.closeInventory();
            return;
        }

        classService.assignClass(player, id);
        player.sendMessage(messages.format("chosen", Map.of("class", id.name())));
        player.closeInventory();
    }
}
