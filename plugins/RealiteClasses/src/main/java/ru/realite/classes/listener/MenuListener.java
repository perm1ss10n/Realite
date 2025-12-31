package ru.realite.classes.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.gui.ClassSettingsMenu;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.HudMode;
import ru.realite.classes.service.ClassHudService;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.service.HiddenClassGate;
import ru.realite.classes.service.HiddenClassGateResult;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class MenuListener implements Listener {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final EvolutionService evolutionService;
    private final HiddenClassGate hiddenClassGate;
    private final Messages messages;
    private final ClassHudService hudService;

    public MenuListener(ClassService classService,
                        ClassConfigRepository classConfig,
                        EvolutionService evolutionService,
                        HiddenClassGate hiddenClassGate,
                        Messages messages,
                        ClassHudService hudService) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.evolutionService = evolutionService;
        this.hiddenClassGate = hiddenClassGate;
        this.messages = messages;
        this.hudService = hudService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (holder == null) return;

        // =========================================================
        // SETTINGS MENU
        // =========================================================
        if (holder instanceof ClassSettingsMenu) {
            e.setCancelled(true);

            if (e.getClickedInventory() == null) return;
            if (e.getClickedInventory().equals(player.getInventory())) return;
            if (e.getCurrentItem() == null) return;

            int slot = e.getRawSlot();
            HudMode mode = switch (slot) {
                case 1 -> HudMode.BOSSBAR;
                case 3 -> HudMode.ACTIONBAR;
                case 5 -> HudMode.SIDEBAR;
                case 7 -> HudMode.OFF;
                default -> null;
            };
            if (mode == null) return;

            var prof = classService.getProfile(player);
            if (prof == null) return;

            prof.setHudMode(mode);
            classService.save(prof);

            if (hudService != null) hudService.refreshNow(player);

            player.sendMessage("§aHUD класса: §f" + mode.name());
            player.closeInventory();
            return;
        }

        // =========================================================
        // CLASS SELECT MENU
        // =========================================================
        if (holder instanceof ClassSelectMenu menu) {
            e.setCancelled(true);

            if (e.getClickedInventory() == null) return;
            if (e.getClickedInventory().equals(player.getInventory())) return;
            if (e.getCurrentItem() == null) return;

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
                HiddenClassGateResult gateResult = hiddenClassGate.check(player, id);
                if (!gateResult.available()) {
                    player.closeInventory();
                    String reasonKey = gateResult.reasonKey();
                    if (reasonKey != null && reasonKey.equals("class-locked-quest")) {
                        String questId = hiddenClassGate.requiredQuestId(id);
                        player.sendMessage(messages.format("class-locked-quest", Map.of(
                                "quest", questId != null ? questId : "-"
                        )));
                    } else if (reasonKey != null && reasonKey.equals("class-locked-evolution")) {
                        player.sendMessage(messages.get("class-locked-evolution"));
                        player.sendMessage(messages.format("class-locked-requirements", Map.of(
                                "req", hiddenClassGate.describeEvolutionRequirement(id)
                        )));
                    } else if (reasonKey != null && reasonKey.equals("class-locked-both")) {
                        player.sendMessage(messages.get("class-locked-both"));
                        String questId = hiddenClassGate.requiredQuestId(id);
                        player.sendMessage(messages.format("class-locked-quest", Map.of(
                                "quest", questId != null ? questId : "-"
                        )));
                        player.sendMessage(messages.format("class-locked-requirements", Map.of(
                                "req", hiddenClassGate.describeEvolutionRequirement(id)
                        )));
                    } else {
                        player.sendMessage(messages.get("class-locked"));
                    }
                    return;
                }
            }

            classService.assignClass(player, id);

            // снимаем "стартовый", раз он выбрал класс вручную
            prof.setStarterClass(false);
            classService.save(prof);

            String niceName = (def != null ? def.name : id.name());
            player.sendMessage(messages.format("chosen", Map.of("class", niceName)));

            // после назначения класса обновим HUD (чтобы сразу сменился текст/прогресс)
            if (hudService != null) hudService.refreshNow(player);

            player.closeInventory();
        }
    }
}
