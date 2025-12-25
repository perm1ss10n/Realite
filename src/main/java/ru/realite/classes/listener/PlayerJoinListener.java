package ru.realite.classes.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.ClassService;

public class PlayerJoinListener implements Listener {

    private final ClassService classService;

    public PlayerJoinListener(ClassService classService) {
        this.classService = classService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var player = e.getPlayer();
        var profile = classService.getProfile(player);

        if (profile == null) return;

        // Если у игрока ещё нет класса — назначаем Странника
        if (!profile.hasClass()) {
            classService.assignClass(player, ClassId.WANDERER);

            profile.setStarterClass(true);
            classService.save(profile);

            player.sendMessage("§6Добро пожаловать, странник.");
            player.sendMessage("§7Ты начал путь с базового класса §eСтранник§7.");
            player.sendMessage("§7Чтобы выбрать другой класс, напиши §a/class choose§7.");
        }
    }
}
