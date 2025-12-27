package ru.realite.classes.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EffectService;

public class PlayerJoinListener implements Listener {

    private final ClassService classService;
    private final EffectService effectService;

    public PlayerJoinListener(ClassService classService, EffectService effectService) {
        this.classService = classService;
        this.effectService = effectService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        var profile = classService.getProfile(player);

        if (profile == null)
            return;

        // Если у игрока ещё нет класса — назначаем Странника
        if (!profile.hasClass()) {
            classService.assignClass(player, ClassId.WANDERER);

            profile.setStarterClass(true);
            classService.save(profile);

            player.sendMessage("§6Добро пожаловать, странник.");
            player.sendMessage("§7Ты начал путь с базового класса §eСтранник§7.");
            player.sendMessage("§7Чтобы выбрать другой класс, напиши §a/class choose§7.");
        }
        // Применяем эффекты класса (или чистим, если класса нет)
        effectService.applyFor(player);
    }
}
